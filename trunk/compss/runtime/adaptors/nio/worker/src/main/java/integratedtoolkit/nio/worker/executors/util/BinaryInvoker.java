package integratedtoolkit.nio.worker.executors.util;

import java.io.File;

import integratedtoolkit.exceptions.InvokeExecutionException;
import integratedtoolkit.nio.NIOTask;
import integratedtoolkit.nio.exceptions.JobExecutionException;
import integratedtoolkit.nio.worker.NIOWorker;

import integratedtoolkit.types.implementations.BinaryImplementation;

import integratedtoolkit.worker.invokers.GenericInvoker;


public class BinaryInvoker extends Invoker {

    private final String binary;


    public BinaryInvoker(NIOWorker nw, NIOTask nt, File taskSandboxWorkingDir, int[] assignedCoreUnits) throws JobExecutionException {
        super(nw, nt, taskSandboxWorkingDir, assignedCoreUnits);

        // Get method definition properties
        BinaryImplementation binaryImpl = null;
        try {
            binaryImpl = (BinaryImplementation) this.impl;
        } catch (Exception e) {
            throw new JobExecutionException(ERROR_METHOD_DEFINITION + this.methodType, e);
        }
        this.binary = binaryImpl.getBinary();
    }

    @Override
    public Object invokeMethod() throws JobExecutionException {
        logger.info("Invoked " + this.binary + " in " + this.nw.getHostName());
        try {
            return GenericInvoker.invokeBinaryMethod(this.binary, this.values, this.hasReturn, this.streams, this.prefixes,
                    this.taskSandboxWorkingDir);
        } catch (InvokeExecutionException iee) {
            throw new JobExecutionException(iee);
        }
    }

}
