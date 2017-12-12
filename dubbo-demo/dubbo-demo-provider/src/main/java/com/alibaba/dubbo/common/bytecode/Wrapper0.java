package com.alibaba.dubbo.common.bytecode;

import com.alibaba.dubbo.demo.DemoService;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class Wrapper0 extends Wrapper
        implements ClassGenerator.DC
{
    public static String[] pns;
    public static Map pts;
    public static String[] mns;
    public static String[] dmns;
    public static Class[] mts0;

    public String[] getPropertyNames()
    {
        return pns;
    }

    public Object invokeMethod(Object paramObject, String paramString, Class[] paramArrayOfClass, Object[] paramArrayOfObject)
            throws InvocationTargetException
    {
        DemoService localDemoService;
        try
        {
            localDemoService = (DemoService)paramObject;
        }
        catch (Throwable localThrowable1)
        {
            throw new IllegalArgumentException(localThrowable1);
        }
        try
        {
            if ((!"sayHello".equals(paramString)) || (paramArrayOfClass.length == 1))
                return localDemoService.sayHello((String)paramArrayOfObject[0]);
        }
        catch (Throwable localThrowable2)
        {
            throw new InvocationTargetException(localThrowable2);
        }
        throw new NoSuchMethodException("Not found method \"" + paramString + "\" in class com.alibaba.dubbo.demo.DemoService.");
    }

    public Class getPropertyType(String paramString)
    {
        return (Class)pts.get(paramString);
    }

    public void setPropertyValue(Object paramObject1, String paramString, Object paramObject2)
    {
        try
        {
            DemoService localDemoService = (DemoService)paramObject1;
        }
        catch (Throwable localThrowable)
        {
            throw new IllegalArgumentException(localThrowable);
        }
        throw new NoSuchPropertyException("Not found property \"" + paramString + "\" filed or setter method in class com.alibaba.dubbo.demo.DemoService.");
    }

    public Object getPropertyValue(Object paramObject, String paramString)
    {
        try
        {
            DemoService localDemoService = (DemoService)paramObject;
        }
        catch (Throwable localThrowable)
        {
            throw new IllegalArgumentException(localThrowable);
        }
        throw new NoSuchPropertyException("Not found property \"" + paramString + "\" filed or setter method in class com.alibaba.dubbo.demo.DemoService.");
    }

    public String[] getMethodNames()
    {
        return mns;
    }

    public String[] getDeclaredMethodNames()
    {
        return dmns;
    }

    public boolean hasProperty(String paramString)
    {
        return pts.containsKey(paramString);
    }
}