package com.izuiyou;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.hook.hookzz.*;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import com.sun.jna.Pointer;

import java.io.File;
import java.nio.charset.StandardCharsets;


public class zuiyou extends AbstractJni{
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final DvmClass NativeClass;

    zuiyou() {
        emulator = AndroidEmulatorBuilder.for32Bit().build(); // 创建模拟器实例
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/izuiyou/right573.apk")); // 创建Android虚拟机
        DalvikModule dm = vm.loadLibrary("net_crypto", true); // 加载so到虚拟内存
        module = dm.getModule(); //获取本SO模块的句柄

        vm.setJni(this);
        vm.setVerbose(true);
        dm.callJNI_OnLoad(emulator);

        NativeClass = vm.resolveClass("com/izuiyou/network/NetCrypto");

//        emulator.traceRead(0x40358000,0x40358000+7);
        emulator.traceRead(0xbffff54cL,0xbffff54cL+0x7L);
        emulator.traceRead(0xbffff63cL,0xbffff63cL+0x7L);
    };


    public void callInit(){
        String methodSign = "native_init()V";
        NativeClass.callStaticJniMethodObject(emulator, methodSign);
    }

    private void callSign(){
        String methodSign = "sign(Ljava/lang/String;[B)Ljava/lang/String;";
        StringObject ret = NativeClass.callStaticJniMethodObject(emulator, methodSign, "12345", "lilac".getBytes(StandardCharsets.UTF_8));
        System.out.println("callSign--> " + ret);
    };

    public static void main(String[] args) throws Exception {
        zuiyou test = new zuiyou();
        //test.hookMemcpy();
        //test.HookMemcmp();
        //test.callInit();
        test.callSign();
    }

    public void hookMemcpy(){
//        void *memcpy(void *str1, const void *str2, size_t n)
//        str1 -- 指向用于存储复制内容的目标数组，类型强制转换为 void* 指针。
//        str2 -- 指向要复制的数据源，类型强制转换为 void* 指针。
//        n -- 要被复制的字节数。
        emulator.attach().addBreakPoint(module.findSymbolByName("memcpy").getAddress(), new BreakPointCallback() {
            // onEnter
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                RegisterContext registerContext = emulator.getContext();
                UnidbgPointer str1 = registerContext.getPointerArg(0);
                UnidbgPointer str2 = registerContext.getPointerArg(1);
                int length = registerContext.getIntArg(2);
                Inspector.inspect(str2.getByteArray(0, length), "要复制的数据源");
                System.out.println("复制到的地方："+str1.toString());
                return true;
            }
        });
    }

    // hook C 库函数
    // int memcmp(const void *str1, const void *str2, size_t n)) 把存储区 str1 和存储区 str2 的前 n 个字节进行比较。
    public void HookMemcmp(){
        emulator.attach().addBreakPoint(module.findSymbolByName("memcmp").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("call memcmp 作比较");
                RegisterContext registerContext = emulator.getContext();
                UnidbgPointer arg1 = registerContext.getPointerArg(0);
                UnidbgPointer arg2 = registerContext.getPointerArg(1);
                int size = registerContext.getIntArg(2);
                Inspector.inspect(arg1.getByteArray(0, size), "arg1");
                Inspector.inspect(arg2.getByteArray(0, size), "arg2");

                if(arg1.getString(0).equals("Context")){
                    emulator.attach().debug();
                }
                return true;
            }
        });
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            // cn.xiaochuankeji.tieba.AppController@1793a3b
            case "com/izuiyou/common/base/BaseApplication->getAppContext()Landroid/content/Context;":{
                return vm.resolveClass("android/content/Context").newObject(null);
            }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "android/content/Context->getClass()Ljava/lang/Class;":{
                return dvmObject.getObjectType();
            }
            // OK
            case "java/lang/Class->getSimpleName()Ljava/lang/String;":{
                return new StringObject(vm, "Context");
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
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public boolean callStaticBooleanMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "android/os/Debug->isDebuggerConnected()Z":{
                return false;
            }
        }
        return super.callStaticBooleanMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public void callStaticVoidMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "cn/xiaochuankeji/tieba/common/debug/AppLogReporter->reportAppRuntime(Ljava/lang/String;Ljava/lang/String;)V":{
                return;
            }
        }
        throw new UnsupportedOperationException(signature);
    }

    @Override
    public int callStaticIntMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "android/os/Process->myPid()I":{
                return emulator.getPid();
            }
        }
        return super.callStaticIntMethodV(vm, dvmClass, signature, vaList);
    }
}

