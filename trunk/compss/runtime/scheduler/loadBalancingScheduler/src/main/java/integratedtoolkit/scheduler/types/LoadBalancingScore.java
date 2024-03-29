package integratedtoolkit.scheduler.types;

import java.util.HashSet;

import integratedtoolkit.comm.Comm;
import integratedtoolkit.scheduler.types.Score;
import integratedtoolkit.types.TaskDescription;
import integratedtoolkit.types.annotations.parameter.Direction;
import integratedtoolkit.types.data.DataAccessId;
import integratedtoolkit.types.data.DataInstanceId;
import integratedtoolkit.types.data.LogicalData;
import integratedtoolkit.types.parameter.DependencyParameter;
import integratedtoolkit.types.parameter.Parameter;
import integratedtoolkit.types.resources.Resource;
import integratedtoolkit.types.resources.Worker;


public class LoadBalancingScore extends ReadyScore {

    /**
     * Creates a new ResourceEmptyScore with the given values
     * 
     * @param actionScore
     * @param waiting
     * @param res
     * @param impl
     */
    public LoadBalancingScore(double actionScore, double res, double waiting, double impl) {
        super(actionScore, res, waiting, impl);
    }

    /**
     * Creates a copy of the @clone ResourceEmptyScore
     * 
     * @param clone
     */
    public LoadBalancingScore(LoadBalancingScore clone) {
        super(clone);
    }

    /**
     * Calculates the score for a given worker @w and a given parameters @params
     * 
     * @param params
     * @param w
     * @return
     */
    @Override
    public double calculateResourceScore(TaskDescription params, Worker<?, ?> w) {
        long resourceScore = 0;
        if (params != null) {
            Parameter[] parameters = params.getParameters();
            if (parameters.length == 0) {
                return 0.5;
            }
            // Obtain the scores for the host: number of task parameters that
            // are located in the host
            for (Parameter p : parameters) {
                if (p instanceof DependencyParameter && p.getDirection() != Direction.OUT) {
                    DependencyParameter dp = (DependencyParameter) p;
                    DataInstanceId dId = null;
                    switch (dp.getDirection()) {
                        case IN:
                            DataAccessId.RAccessId raId = (DataAccessId.RAccessId) dp.getDataAccessId();
                            dId = raId.getReadDataInstance();
                            break;
                        case INOUT:
                            DataAccessId.RWAccessId rwaId = (DataAccessId.RWAccessId) dp.getDataAccessId();
                            dId = rwaId.getReadDataInstance();
                            break;
                        case OUT:
                            // Cannot happen because of previous if
                            break;
                    }

                    // Get hosts for resource score
                    if (dId != null) {
                        LogicalData dataLD = Comm.getData(dId.getRenaming());
                        if (dataLD != null) {
                            HashSet<Resource> hosts = dataLD.getAllHosts();
                            for (Resource host : hosts) {
                                if (host == w) {
                                    resourceScore++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return resourceScore;
    }

    @Override
    public boolean isBetter(Score other) {
        if (this.actionScore != other.actionScore) {
            return this.actionScore > other.actionScore;
        }
        if (this.resourceScore != other.resourceScore) {
            return this.resourceScore > other.resourceScore;
        }
        if (this.implementationScore != other.implementationScore) {
            return this.implementationScore > other.implementationScore;
        }
        return this.waitingScore > other.waitingScore;
    }

    @Override
    public String toString() {
        return "[LoadBalancingScore = [action:" + actionScore + ", resource:" + resourceScore + ", load:" + waitingScore
                + ", implementation:" + implementationScore + "]" + "]";
    }

}
