package org.example;

import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardBranchedFlowAnalysis;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.*;

public class SimpleBranchAnalysis extends ForwardBranchedFlowAnalysis<FlowSet<RawOperator>> {

    private static int MAX = 100;

    private boolean stop = false;
    private final SootMethod sm;
    private final List<String> someSelfFunctions = new ArrayList<>();
    private boolean isRoot = true;

    private final DirectedGraph graph;
    private SimpleBranchAnalysis rootAnalysis = null;



    private SimpleBranchAnalysis(SootMethod sm, UnitGraph graph, SimpleBranchAnalysis rootAnalysis){
        super(graph);
        this.sm = sm;
        this.graph = graph;
        if (rootAnalysis!=null){
            this.rootAnalysis = rootAnalysis;
            this.isRoot = false;
        }else{
            someSelfFunctions.add(sm.toString());
        }
        doAnalysis();
     }
    public SimpleBranchAnalysis(SootMethod sm , UnitGraph graph) {
       this(sm, graph, null);
    }

    private static boolean isSootMethodFlowNeeded(SootMethod sootMethod){
        String className = sootMethod.getDeclaringClass().getName();
        if (className.equals("android.os.Parcel")){
            return true;
        }
        return false;
    }

    @Override
    protected FlowSet<RawOperator>  newInitialFlow() {
        return new ArraySparseSet<>();
    }

    @Override
    protected void merge(FlowSet<RawOperator> src1, FlowSet<RawOperator> src2, FlowSet<RawOperator> dest) {
        src1.union(src2,dest);
    }

    @Override
    protected void copy(FlowSet<RawOperator> in, FlowSet<RawOperator> out) {
        in.copy(out);
    }


    @Override
    protected void flowThrough(FlowSet<RawOperator> units, Unit unit, List<FlowSet<RawOperator>> fallout, List<FlowSet<RawOperator>> branchOut) {
        if (units.size() > MAX){
            stop = true;
        }

        if (stop){
            units = newInitialFlow();
        }
        SootMethod target = null;
        FlowSet<RawOperator> innerResult = null;
//        System.out.println(sm.toString()+"\\" + unit + " " + units);
        if (unit instanceof JAssignStmt) {
            Value left = ((JAssignStmt) unit).getLeftOp();
            Value right = ((JAssignStmt) unit).getRightOp();
            if (right instanceof InvokeExpr) {
                SootMethod sonMethod = ((InvokeExpr) right).getMethod();
                if(isSootMethodFlowNeeded(sonMethod)){
                    target = sonMethod;
                } else {
                    innerResult = enterFunctionCallIfNeeded(sonMethod);
                }
            }
        } else if (unit instanceof JInvokeStmt) {
            SootMethod sonMethod = ((JInvokeStmt) unit).getInvokeExpr().getMethod();
            if(isSootMethodFlowNeeded(sonMethod)){
                target = sonMethod;
            }else {
                innerResult = enterFunctionCallIfNeeded(sonMethod);
            }
        }
        FlowSet<RawOperator> newValue = units.clone();
        if (target != null){
            RawOperator op = RawOperator.parseRawOperatorFromSooetMethod(target);
            if (op!=null)
                newValue.add(op);
        } else if (innerResult != null) {
            newValue.union(innerResult);
        }


        Iterator<FlowSet<RawOperator>> it = fallout.iterator();
        while (it.hasNext()){
            FlowSet<RawOperator> u = it.next();
            copy(newValue, u);
        }


        it = branchOut.iterator();
        while (it.hasNext()){
            FlowSet<RawOperator> u = it.next();
            copy(newValue, u);
        }
//        copy(newValue, out);
    }


    private FlowSet<RawOperator> enterFunctionCallIfNeeded(SootMethod sonMethod){
        FlowSet<RawOperator> result = newInitialFlow();
        for (Type type : sonMethod.getParameterTypes()){
            String typeString = type.toString();
                if (typeString.equals("android.os.Parcel")){
                        if(!putSelfFuncAndCheckContains(sonMethod)){
                            try{
                                Body body = sonMethod.retrieveActiveBody();
                                if (body != null){
                                    SimpleBranchAnalysis inner = new SimpleBranchAnalysis(sonMethod, new ExceptionalUnitGraph(body), getRoot());
                                    result = inner.getResult();
                                }
                            }catch (RuntimeException e){
                                //igonre some method!
                            }
                        }
                        break;
                }
        }
        return result;
    }

    public boolean putSelfFuncAndCheckContains(SootMethod sootMethod){
        List<String> selfFunctions = isRoot ? this.someSelfFunctions : rootAnalysis.someSelfFunctions;

        if(!selfFunctions.contains(sootMethod.toString()))
        {
            selfFunctions.add(sootMethod.toString());
            return false;
        }
        return true;
    }

    public FlowSet<RawOperator> getResult(){
        Body body = sm.retrieveActiveBody();
        List<Unit> tails = graph.getTails();
        FlowSet<RawOperator> maxResult = new ArraySparseSet<>();

        for (Unit tail : tails){
            FlowSet<RawOperator> result = this.getFlowBefore(tail);
            if (maxResult.size() < result.size())
                maxResult = result;
        }
//        System.out.println("unit is " + unit);
        return maxResult;
    }


    public boolean isStop() {
        return stop;
    }

    public SimpleBranchAnalysis getRoot(){
        if (isRoot)
            return this;
        else
            return rootAnalysis;
    }
}
