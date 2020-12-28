package com.john.android.xuimodx

import android.view.View
import android.view.animation.ScaleAnimation
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * @Author: liuqiang
 * @Date: 12/28/20 9:38 AM
 * @Description:
 */

class MainHook:IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        var r: Class<*>?
        var l:Class<*>? = null
        var h :Class<*>? = null
        var v :String? = null
        var b :String? = null

        if(lpparam==null) return
        else{
            val activityThread = XposedHelpers.findClass("android.app.ActivityThread", lpparam.classLoader)

            XposedBridge.hookAllMethods(activityThread,"performLaunchActivity",object :XC_MethodHook(){
                override fun afterHookedMethod(param: MethodHookParam?) {
                    if(param==null) return
                    val mInitialApplication = XposedHelpers.getObjectField(param.thisObject,"mInitialApplication")
                    val  finalCL = XposedHelpers.callMethod(mInitialApplication,"getClassLoader") as ClassLoader
                    XposedBridge.log("found classload is => $finalCL")

                    r = XposedHelpers.findClassIfExists("androidx.recyclerview.widget.RecyclerView",finalCL)

                    if(r==null) XposedBridge.log("FOUND NO RECYCLERVIEW")
                    r?.let {
                        // 找到 Recycler 的 Class
                        for (i in it.classes.indices)
                            if(it.classes[i].modifiers == 17){
                                l = XposedHelpers.findClass(it.classes[i].name,finalCL)
                                XposedBridge.log("--------\nFOUND Recycler\n---------\n")
                                break
                            }

                        // 找到 tryBind 的真实 name
                        l?.let { recycler ->

                            for (i in recycler.declaredMethods.indices) {
                                if(recycler.declaredMethods[i].modifiers==2
                                    &&recycler.declaredMethods[i].parameterTypes.size==4
                                    && recycler.declaredMethods[i].returnType == Boolean::class.java){
                                    b = recycler.declaredMethods[i].name
                                    XposedBridge.log("--------\nFOUND tryBind\n---------\n")
                                    break
                                }
                            }
                        }
                        // 找到 ViewHolder 的 Class
                        for (i in it.methods.indices)
                            if(it.methods[i].parameterTypes.size==1 && it.methods[i].parameterTypes[0] == Long::class.java){
                                h = XposedHelpers.findClass(it.methods[i].returnType.name,finalCL)
                                XposedBridge.log("--------\nFOUND ViewHolder\n---------\n")
                                break
                            }
                        // 找到itemView的真实name
                        h?.let { holder->
                            for (i in holder.fields.indices){
                                if(holder.fields[i].modifiers == 17 && holder.fields[i].type == View::class.java){
                                    v = holder.fields[i].name
                                    XposedBridge.log("--------\nFOUND 找到itemView的真实name\n---------\n")
                                    break
                                }
                            }
                        }
                    }

                    r?.let {
                        l?.let { recycler->
                            h?.let { viewHolder->
                                v?.let { itemView->
                                    b?.let { tryBind->
                                        XposedBridge.log("--------\nFINAL HOOK\n---------\n")
                                        XposedHelpers.findAndHookMethod(recycler,tryBind,viewHolder,Int::class.java
                                            ,Int::class.java,Long::class.java,object :XC_MethodHook(){

                                                override fun afterHookedMethod(param: MethodHookParam?) {
                                                    param?.let {
                                                        XposedHelpers.getObjectField(it.args[0],itemView)?.let {v->
                                                            (v as View).startAnimation(ScaleAnimation(0.2f, 1f, 1f, 1f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f).apply {
                                                                this.duration = 333
                                                            })
                                                        }
                                                    }

                                                }
                                            })
                                    }
                                }
                            }
                        }
                    }
                }
            })
        }
    }
}