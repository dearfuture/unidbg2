package com.izuiyou;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.arm.backend.DynarmicFactory;

import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.Arm32RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.Module;
import com.sina.weibo.Weibo;
import com.sun.jna.Pointer;
import unicorn.ArmConst;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;


public class NetWork  extends AbstractJni{

    public static void main(String[] args) {
        NetWork nw = new NetWork();
        String result = nw.calls();
        System.out.println("call sign result:"+result);
    }

    private String calls() {
        //实例方法callJniMethodObject传conetxt
        //DvmObject<?> context = vm.resolveClass("android/app/Application", vm.resolveClass("android/content/ContextWrapper", vm.resolveClass("android/content/Context"))).newObject(null);

        String arg1 = "hello world";
        String arg2 = "V I 50";

        //静态方法直接调用callStaticJniMethodObject
        String ret = NativeApi.callStaticJniMethodObject(emulator, "sign(Ljava/lang/String;[B)Ljava/lang/String;", arg1, arg2.getBytes(StandardCharsets.UTF_8)).getValue().toString();
        return ret;
    }

    private final AndroidEmulator emulator;
    private final VM vm;
    //private final Module module;
    private final DvmClass NativeApi;

    private NetWork() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.addBackendFactory(new DynarmicFactory(true))
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("cn.xiaochuankeji.tieba")
                .build();
        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/izuiyou/right573.apk"));

        vm.setJni(this);
        vm.setVerbose(true);
        //DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/izuiyou/libnet_crypto.so"), true);
        DalvikModule dm = vm.loadLibrary("net_crypto", true);

        NativeApi = vm.resolveClass("com/izuiyou/network/NetCrypto");
        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "com/izuiyou/common/base/BaseApplication->getAppContext()Landroid/content/Context;":{
                // return vm.resolveClass("android/content/Context").newObject(null);
                //DvmObject<?> context = vm.resolveClass("android/app/Application", vm.resolveClass("android/content/ContextWrapper", vm.resolveClass("android/content/Context"))).newObject(null);

                //关键点
                DvmObject<?> context = vm.resolveClass("cn/xiaochuankeji/tieba/AppController").newObject(null);
                return context;
            }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

//    @Override
//    public void callStaticVoidMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
//        switch (signature){
//            case "cn/xiaochuankeji/tieba/common/debug/AppLogReporter->reportAppRuntime(Ljava/lang/String;Ljava/lang/String;)V":{
//                return;
//            }
//        }
//        throw new UnsupportedOperationException(signature);
//    }

    @Override
    public int callStaticIntMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "android/os/Process->myPid()I":{
                return emulator.getPid();
            }
        }
        throw new UnsupportedOperationException(signature);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "java/lang/Class->getClass()Ljava/lang/Class;":{
                return dvmObject.getObjectType();
            }
            case "java/lang/Class->getSimpleName()Ljava/lang/String;":{
                String className = ((DvmClass) dvmObject).getClassName();
                String[] name = className.split("/");
                return new StringObject(vm, name[name.length - 1]);
            }
            case "java/lang/Class->getFilesDir()Ljava/io/File;":{
                return vm.resolveClass("java/io/File").newObject(signature);
            }
            case "android/content/Context->getFilesDir()Ljava/io/File;":{
                return vm.resolveClass("java/io/File").newObject(signature);
            }
            case "java/io/File->getAbsolutePath()Ljava/lang/String;":{
                String tag = dvmObject.getValue().toString();
                if(tag.equals("android/content/Context->getFilesDir()Ljava/io/File;")){
                    return new StringObject(vm, "/data/data/"+vm.getPackageName()+"/files");
                }
            }

            case "java/lang/Class->getPackageName()Ljava/lang/String;":{
                String packageName = vm.getPackageName();
                if (packageName != null) {
                    return new StringObject(vm, packageName);
                }
            }
            case "java/lang/Class->getPackageManager()Landroid/content/pm/PackageManager;":{
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
            }

            case "cn/xiaochuankeji/tieba/AppController->getPackageManager()Landroid/content/pm/PackageManager;": {
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
            }
            case "cn/xiaochuankeji/tieba/AppController->getPackageName()Ljava/lang/String;": {
                String packageName = vm.getPackageName();
                if (packageName != null) {
                    return new StringObject(vm, packageName);
                }
            }
            case "cn/xiaochuankeji/tieba/AppController->getClass()Ljava/lang/Class;": {
                return dvmObject.getObjectType();
            }
            case "cn/xiaochuankeji/tieba/AppController->getFilesDir()Ljava/io/File;": {
                return vm.resolveClass("java/io/File").newObject(signature);
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature){
            case "java/security/MessageDigest->digest([B)[B":{
                MessageDigest messageDigest = (MessageDigest) dvmObject.getValue();
                byte[] digest = messageDigest.digest();
                DvmObject<?> object = ProxyDvmObject.createObject(vm ,digest);
                vm.addLocalObject(object);
                return object;
            }
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public boolean callStaticBooleanMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "android/os/Debug->isDebuggerConnected()Z": {
                return false;
            }
        }
        throw new UnsupportedOperationException(signature);
    }
}
