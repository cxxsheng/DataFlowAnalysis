package org.example;

import soot.*;
import soot.jimple.StaticFieldRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.FlowSet;

import java.util.*;

public class Main {

    public static String[] hitStrings = {
            "writeArray",
            "writeBinderArray",
            "writeBinderList",
            "writeBlob",
            "writeBlob",
            "writeBoolean",
            "writeBooleanArray",
            "writeBundle",
            "writeByte",
            "writeByteArray",
            "writeByteArray",
            "writeCharArray",
            "writeDouble",
            "writeDoubleArray",
            "writeException",
            "writeFileDescriptor",
            "writeFixedArray",
            "writeFloat",
            "writeFloatArray",
            "writeInt",
            "writeIntArray",
            "writeInterfaceArray",
            "writeInterfaceList",
            "writeInterfaceToken",
            "writeList",
            "writeLong",
            "writeLongArray",
            "writeMap",
            "writeNoException",
            "writeParcelable",
            "writeParcelableArray",
            "writeParcelableCreator",
            "writeParcelableList",
            "writePersistableBundle",
            "writeSerializable",
            "writeSize",
            "writeSizeF",
            "writeSparseArray",
            "writeSparseBooleanArray",
            "writeString",
            "writeStringArray",
            "writeStringList",
            "writeStrongBinder",
            "writeStrongInterface",
            "writeTypedArray",
            "writeTypedArrayMap",
            "writeTypedList",
            "writeTypedList",
            "writeTypedObject",
            "writeTypedSparseArray",
            "writeValue",
    };



    private static void setExclude(){
        List<String> excudeList = new ArrayList();
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_exclude(excudeList);
    }

    private static List<String> someSelfFunction = new ArrayList<>();




    private static String getListString(List<String> list){

        StringBuilder sb = new StringBuilder("{");
        for (String s : list){
            sb.append(s).append(", ");
        }
        if (!list.isEmpty())
            sb.delete(sb.length()-2, sb.length());
        sb.append("}");
        return sb.toString();
    }

    private static Map<String,String> WhiteList = new HashMap();

    public static boolean compareTwoReturns(FlowSet<RawOperator> writeAnalysisRet, FlowSet<RawOperator> createAnalysisRet){
        Iterator<RawOperator> writeIterator = writeAnalysisRet.iterator();
        Iterator<RawOperator> createIterator = createAnalysisRet.iterator();

        if (writeAnalysisRet.size() == createAnalysisRet.size() && writeAnalysisRet.size() > 0){
            while (writeIterator.hasNext() && createIterator.hasNext()){
               RawOperator writeOp = writeIterator.next();
               RawOperator readOp = createIterator.next();
               if(!writeOp.match(readOp)){
                   return false;
               }
            }
            return true;
        }else
        {
            return false;
        }

    }
    public static List LoadClass(String p){
        List<Body> rets = new ArrayList<>();
        Options.v().set_src_prec(Options.src_prec_apk_c_j);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_process_dir(Arrays.asList(p));
        Options.v().set_process_multiple_dex(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        setExclude();
        ArrayList<SootClass> sootClasses = new ArrayList<>();
        Scene.v().loadNecessaryClasses(); // may take dozens of seconds
        Logger.log("Class sizing... " + Scene.v().getApplicationClasses().size());
        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            if (clazz.toString().startsWith("androidx.") || clazz.toString().startsWith("android."))
                continue;
            if (clazz.isInterface())
                continue;
            for (SootClass interfaze : clazz.getInterfaces())
            {
                if ( "android.os.Parcelable".equals(interfaze.toString())){
                    sootClasses.add(clazz);
                    String methodSig = "void writeToParcel(android.os.Parcel,int)";
                    SootMethod method = clazz.getMethodUnsafe(methodSig);
                    if (method == null){
                        Logger.log("code wrong with " + clazz + " cannot find writeToParcel");
                        break;
                    }
                    Logger.log("calling " + methodSig + " in "+ clazz);
                    Body body = method.retrieveActiveBody();
                    SimpleBranchAnalysis writeAnalysis = new SimpleBranchAnalysis(method,new ExceptionalUnitGraph(body));
                    FlowSet<RawOperator> writeAnalysisRet = writeAnalysis.getResult();
                    SootMethod clint = clazz.getMethodUnsafe("void <clinit>()");
                    if (clint == null)
                    {
                        Logger.log("code wrong with " + clazz + " cannot find clinit");
                        break;
                    }
                    body =  clint.retrieveActiveBody();
                    SootClass creatorClass = null;
                    for (Unit unit : body.getUnits()) {
                        if (unit instanceof JAssignStmt){
                            Value left = ((JAssignStmt) unit).getLeftOp();
                            Value right = ((JAssignStmt) unit).getRightOp();
                            if (right instanceof JCastExpr){
                                if (right instanceof  RefType)
                                {
                                    RefType rt = (RefType) right.getType();
                                    if(rt.getClassName().equals("android.os.Parcelable$Creator"))
                                    {
                                        RefType type = (RefType) ((JCastExpr) right).getOp().getType();
                                        creatorClass = type.getSootClass();
                                        break;
                                    }
                                }
                            }
                            if (left instanceof StaticFieldRef){
                                if(((StaticFieldRef) left).getFieldRef().name().equals("CREATOR")){
                                    RefType type = (RefType) right.getType();
                                    creatorClass = type.getSootClass();
                                    break;
                                }
                            }
                        }
                    }
                    if (creatorClass!= null && !creatorClass.getName().startsWith("android.")){
                        methodSig = "java.lang.Object createFromParcel(android.os.Parcel)";
                        method = creatorClass.getMethodUnsafe(methodSig);
                        if (method == null)
                        {
                            Logger.log("cannot find " + methodSig + " in "+ creatorClass);
                            continue;
                        }
                        Logger.log("calling " + methodSig + " in "+ creatorClass);
                        Body createBody = method.retrieveActiveBody();
                        SimpleBranchAnalysis createAnalysis = new SimpleBranchAnalysis(method, new ExceptionalUnitGraph(createBody));
                        FlowSet<RawOperator> createAnalysisRet = createAnalysis.getResult();
                        if (createAnalysis.isStop() || writeAnalysis.isStop()){
                            Logger.log("unknown stop " + clazz);
                        }else if (!compareTwoReturns(writeAnalysisRet,createAnalysisRet)){
                           Logger.log("false "+ clazz +" " + createAnalysisRet + " vs "  + writeAnalysisRet);
                        }else {
                           Logger.log("true " + clazz);
                        }
                     }else{
                        Logger.log("code wrong with " + clazz + " cannot find creator");
                    }
                    break;
                }
            }
        }

        for (SootClass clazz: sootClasses){
            System.out.println("\""+clazz.getName()+"\", ");
        }
        return rets;
    }


    public static void main(String[] args) {
        Logger.log("start handling " + args[0] + " ...");
        LoadClass(args[0]);
    }
}