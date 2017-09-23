/*
 * ****************************************************************************
 * Copyright VMware, Inc. 2010-2016.  All Rights Reserved.
 * ****************************************************************************
 *
 * This software is made available for use under the terms of the BSD
 * 3-Clause license:
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.vmware.scheduling;

import com.vmware.common.annotations.Option;
import com.vmware.common.annotations.Sample;
import com.vmware.connection.ConnectedVimServiceBase;
import com.vmware.vim25.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * WeeklyRecurrenceScheduledTask
 *
 * This sample demonstrates creation of weekly recurring ScheduledTask
 * using the ScheduledTaskManager
 *
 * <b>Parameters:</b>
 * url         [required] : url of the web service
 * username    [required] : username for the authentication
 * password    [required] : password for the authentication
 * vmname      [required] : virtual machine to be powered off
 * taskname    [required] : Name of the task to be created
 *
 * <b>Command Line:</b>
 * run.bat com.vmware.scheduling.WeeklyRecurrenceScheduledTask
 * --url [webserviceurl] --username [username] --password [password]
 * --vmname [VM name] --taskname [TaskToBeCreated]
 * </pre>
 */

@Sample(
        name = "weekly-scheduled-task",
        description = "This sample demonstrates " +
                "creation of weekly recurring ScheduledTask " +
                "using the ScheduledTaskManager. " +
                "This sample will create a task to " +
                "Reboot Guest VM's at 11.59 pm every Saturday"
)
public class WeeklyRecurrenceScheduledTask extends ConnectedVimServiceBase {
    private ManagedObjectReference propCollectorRef;
    private ManagedObjectReference virtualMachine;
    private ManagedObjectReference scheduleManager;

    String vmName = null;
    String taskName = null;


    @Option(name = "vmname", description = "virtual machine to be powered off")
    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    @Option(name = "taskname", description = "Name of the task to be created")
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    /**
     * Create method action to reboot the guest in a vm.
     *
     * @return the action to run when the schedule runs
     */
    private Action createTaskAction() {
        MethodAction action = new MethodAction();

        // Method Name is the WSDL name of the
        // ManagedObject's method to be run, in this Case,
        // the rebootGuest method for the VM
        action.setName("RebootGuest");
        return action;
    }

    /**
     * Create a Weekly task scheduler to run at 11:59 pm every Saturday.
     *
     * @return weekly task scheduler
     */
    private TaskScheduler createTaskScheduler() {
        WeeklyTaskScheduler scheduler = new WeeklyTaskScheduler();

        // Set the Day of the Week to be Saturday
        scheduler.setSaturday(true);

        // Set the Time to be 23:59 hours or 11:59 pm
        scheduler.setHour(23);
        scheduler.setMinute(59);

        // set the interval to 1 to run the task only
        // Once every Week at the specified time
        scheduler.setInterval(1);

        return scheduler;
    }

    /**
     * Create a Scheduled Task using the reboot method action and the weekly
     * scheduler, for the VM found.
     *
     * @param taskAction action to be performed when schedule executes
     * @param scheduler  the scheduler used to execute the action
     * @throws Exception
     */
    void createScheduledTask(Action taskAction, TaskScheduler scheduler) throws DuplicateNameFaultMsg, RuntimeFaultFaultMsg, InvalidNameFaultMsg {
        // Create the Scheduled Task Spec and set a unique task name
        // and description, and enable the task as soon as it is created
        ScheduledTaskSpec scheduleSpec = new ScheduledTaskSpec();
        scheduleSpec.setName(taskName);
        scheduleSpec
                .setDescription("Reboot VM's Guest at 11.59 pm every Saturday");
        scheduleSpec.setEnabled(true);

        // Set the RebootGuest Method Task action and
        // the Weekly scheduler in the spec
        scheduleSpec.setAction(taskAction);
        scheduleSpec.setScheduler(scheduler);

        // Create the ScheduledTask for the VirtualMachine we found earlier
        ManagedObjectReference task =
                vimPort.createScheduledTask(scheduleManager, virtualMachine,
                        scheduleSpec);

        // printout the MoRef id of the Scheduled Task
        System.out.println("Successfully created Weekly Task: "
                + task.getValue());
    }

    // Action conflicts with Action here
    @com.vmware.common.annotations.Action
    public void run() throws DuplicateNameFaultMsg, RuntimeFaultFaultMsg, InvalidNameFaultMsg, InvalidPropertyFaultMsg {
        propCollectorRef = serviceContent.getPropertyCollector();
        scheduleManager = serviceContent.getScheduledTaskManager();
        // find the VM by dns name to create a scheduled task for
        Map<String, ManagedObjectReference> vms = getMOREFs.inContainerByType(serviceContent
                .getRootFolder(), "VirtualMachine");
        virtualMachine = vms.get(vmName);
        // create the power Off action to be scheduled
        Action taskAction = createTaskAction();

        // create a One time scheduler to run
        TaskScheduler taskScheduler = createTaskScheduler();

        // Create Scheduled Task
        createScheduledTask(taskAction, taskScheduler);
    }
}
