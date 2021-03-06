package integratedtoolkit.nio.worker.executors.util;

import java.io.File;

import integratedtoolkit.exceptions.InvokeExecutionException;
import integratedtoolkit.nio.NIOTask;
import integratedtoolkit.nio.exceptions.JobExecutionException;
import integratedtoolkit.nio.worker.NIOWorker;

import integratedtoolkit.types.implementations.OmpSsImplementation;

import integratedtoolkit.worker.invokers.GenericInvoker;


public class OmpSsInvoker extends Invoker {

    private final String ompssBinary;


    public OmpSsInvoker(NIOWorker nw, NIOTask nt, File taskSandboxWorkingDir, int[] assignedCoreUnits) throws JobExecutionException {
        super(nw, nt, taskSandboxWorkingDir, assignedCoreUnits);

        // Get method definition properties
        OmpSsImplementation ompssImpl = null;
        try {
            ompssImpl = (OmpSsImplementation) this.impl;
        } catch (Exception e) {
            throw new JobExecutionException(ERROR_METHOD_DEFINITION + this.methodType, e);
        }
        this.ompssBinary = ompssImpl.getBinary();
    }

    @Override
    public Object invokeMethod() throws JobExecutionException {
        logger.info("Invoked " + ompssBinary + " in " + nw.getHostName());
        try {
            return GenericInvoker.invokeOmpSsMethod(this.ompssBinary, this.values, this.hasReturn, this.streams, this.prefixes,
                    this.taskSandboxWorkingDir);
        } catch (InvokeExecutionException iee) {
            throw new JobExecutionException(iee);
        }
    }

}
