package integratedtoolkit.types.parameter;

import integratedtoolkit.types.annotations.Constants;
import integratedtoolkit.types.annotations.parameter.DataType;
import integratedtoolkit.types.annotations.parameter.Direction;
import integratedtoolkit.types.annotations.parameter.Stream;

import java.io.Serializable;


public abstract class Parameter implements Serializable {

    /**
     * Serializable objects Version UID are 1L in all Runtime
     */
    private static final long serialVersionUID = 1L;

    // Parameter fields
    private DataType type;
    private final Direction direction;
    private final Stream stream;
    private final String prefix;


    public Parameter(DataType type, Direction direction, Stream stream, String prefix) {
        this.type = type;
        this.direction = direction;
        this.stream = stream;
        if (prefix == null || prefix.isEmpty()) {
            this.prefix = Constants.PREFIX_EMTPY;
        } else { 
            this.prefix = prefix;
        }
    }

    public DataType getType() {
        return this.type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public Stream getStream() {
        return this.stream;
    }
    
    public String getPrefix() {
        return this.prefix;
    }

}
