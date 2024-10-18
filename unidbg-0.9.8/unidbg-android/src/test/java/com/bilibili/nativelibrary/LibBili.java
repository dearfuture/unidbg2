package com.bilibili.nativelibrary;

import com.github.unidbg.*;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.linux.android.dvm.wrapper.DvmBoolean;
import com.github.unidbg.memory.Memory;
import com.izuiyou.NetWork;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class LibBili extends AbstractJni implements IOResolver {
    public static void main(String[] args) {
        LibBili lb = new LibBili();
        String result = lb.callS();
        System.out.println("call s result:"+result);
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("lilac open:"+pathname);
        return null;
    }

    public String callS(){
        TreeMap<String, String> map = new TreeMap<>();
        map.put("build", "6180500");
        map.put("mobi_app", "android");
        map.put("channel", "shenma069");
        map.put("appkey", "1d8b6e7d45233436");
        map.put("s_locale", "zh_CN");
        DvmObject<?> mapObject = ProxyDvmObject.createObject(vm, map);

//        TreeMap<String, String> keymap = new TreeMap<>() ;
//        keymap.put("ts", "123213213");
//        DvmClass Map = vm.resolveClass("java/util/Map");
//        DvmClass AbstractMap = vm.resolveClass("java/util/AbstractMap",Map);
//        DvmObject<?> mapObject = vm.resolveClass("java/util/TreeMap",AbstractMap).newObject(keymap);


        List<Object> argList = new ArrayList<>(10);
        argList.add(vm.getJNIEnv()); // 第一个参数是env
        argList.add(0); //null
        argList.add(vm.addLocalObject(mapObject));

        //0x1c97从unidbg的RegisterNative的打印信息获取
        //RegisterNative(com/bilibili/nativelibrary/LibBili, s(Ljava/util/SortedMap;)Lcom/bilibili/nativelibrary/SignedQuery;, RX@0x40001c97[libbili.so]0x1c97)
        Number number = module.callFunction(emulator, 0x1c97, argList.toArray());
        DvmObject<?> retObject = vm.getObject(number.intValue());
        String ret = retObject.getValue().toString();
        return ret;
    }

    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final DvmClass NativeApi;

    private LibBili() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.addBackendFactory(new DynarmicFactory(true))
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("tv.danmaku.bili")
                .build();
        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/bilibili/nativelibrary/bilibili.apk"));

        vm.setJni(this);
        vm.setVerbose(true);

        emulator.getSyscallHandler().setVerbose(true);
        //多线程，似乎不开也没问题
        emulator.getSyscallHandler().setEnableThreadDispatcher(true);
        //文件访问处理，似乎不开也没问题
        emulator.getSyscallHandler().addIOResolver(this);

        //DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/bilibili/nativelibrary/libbili.so"), true);
        DalvikModule dm = vm.loadLibrary("bili", true);

        NativeApi = vm.resolveClass("/com/bilibili/nativelibrary/LibBili");
        module = dm.getModule();
        //dm.callJNI_OnLoad(emulator);
        vm.callJNI_OnLoad(emulator, module);
    }

    @Override
    public boolean callBooleanMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature){
            case "java/util/Map->isEmpty()Z":{
                return ((Map) dvmObject.getValue()).isEmpty();
            }
        }

        return super.callBooleanMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature){
            case "java/util/Map->get(Ljava/lang/Object;)Ljava/lang/Object;":{
                Object key = varArg.getObjectArg(0).getValue();
                Map map = (Map) dvmObject.getValue();
                Object value = map.get(key);

                DvmObject<?> object = ProxyDvmObject.createObject(vm ,value);
                vm.addLocalObject(object);
                return object;
            }
            case "java/util/Map->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;" :{
                Object key = varArg.getObjectArg(0).getValue();
                Object value = varArg.getObjectArg(1).getValue();
                Map map = (Map) dvmObject.getValue();
                Object retObject = map.put(key, value);

                DvmObject<?> object = ProxyDvmObject.createObject(vm ,retObject);
                vm.addLocalObject(object);
                return object;
            }
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature){
            case "com/bilibili/nativelibrary/SignedQuery->r(Ljava/util/Map;)Ljava/lang/String;":{
                Map map = (Map) varArg.getObjectArg(0).getValue();
                return new StringObject(vm, SignedQuery.r(map));
            }
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "com/bilibili/nativelibrary/SignedQuery-><init>(Ljava/lang/String;Ljava/lang/String;)V": {
                String str = varArg.getObjectArg(0).getValue().toString();
                String str2 = varArg.getObjectArg(1).getValue().toString();

                //也可以这样
                //DvmObject<?> object = ProxyDvmObject.createObject(vm ,new SignedQuery(str, str2));
                //vm.addLocalObject(object);
                //return object;
                return vm.resolveClass("com/bilibili/nativelibrary/SignedQuery").newObject(new SignedQuery(str, str2));
            }
        }
        return super.newObject(vm, dvmClass, signature, varArg);
    }
}
