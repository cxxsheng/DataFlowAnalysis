package org.example;

import soot.SootMethod;

import java.util.Objects;

public class RawOperator {

    public final static int OP_UNKNOWN = -1;
    public final static int OP_WRITE = 0;
    public final static int OP_READ = 1;

    private final int op;
    private String name;

    private final SootMethod method;
    private RawOperator(int op, String name, SootMethod sm){
        this.op = op;
        this.name = name;
        this.method = sm;
    }

    public static RawOperator parseRawOperatorFromSooetMethod(SootMethod sm){
        String methodName = sm.getName();
        int op = OP_UNKNOWN;
        String name = null;
        if (methodName.startsWith("read")) {
            op = OP_READ;
            name = methodName.substring(4);
        } else if (methodName.startsWith("create")) {
            op = OP_READ;
            name = methodName.substring(6);
        }else if( methodName.startsWith("write")){
            op = OP_WRITE;
            name = methodName.substring(5);
        }

        if (op != OP_UNKNOWN)
            return new RawOperator(op, name, sm);
        else
            return null;
    }


    public String toFullString() {
        return "RawOperator{" +
                "op=" + op +
                ", name='" + name + '\'' +
                ", method=" + method +
                '}';
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean match(RawOperator o) {
        if (o == null) return false;
        if (op == o.op)
            return false;
        RawOperator readOP = op == OP_READ ? this : o;
        readOP.name = readOP.name.replace("ArrayList", "List")
                    .replace("HashMap","Map");
        return name.equals(o.name);
    }


}
