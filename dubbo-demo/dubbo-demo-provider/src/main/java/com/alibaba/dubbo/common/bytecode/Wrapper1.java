package com.alibaba.dubbo.common.bytecode;

import com.alibaba.dubbo.demo.provider.DemoServiceImpl;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class Wrapper1 extends Wrapper
        implements ClassGenerator.DC
{
    public static String[] pns;
    public static Map pts;
    public static String[] mns;
    public static String[] dmns;
    public static Class[] mts0;
    public static Class[] mts1;

    public boolean hasProperty(String paramString)
    {
        return pts.containsKey(paramString);
    }

    public Class getPropertyType(String paramString)
    {
        return (Class)pts.get(paramString);
    }

    public void setPropertyValue(Object paramObject1, String paramString, Object paramObject2)
    {
        try
        {
            DemoServiceImpl localDemoServiceImpl = (DemoServiceImpl)paramObject1;
        }
        catch (Throwable localThrowable)
        {
            throw new IllegalArgumentException(localThrowable);
        }
        throw new NoSuchPropertyException("Not found property \"" + paramString + "\" filed or setter method in class com.alibaba.dubbo.demo.provider.DemoServiceImpl.");
    }

    public Object getPropertyValue(Object paramObject, String paramString)
    {
        try
        {
            DemoServiceImpl localDemoServiceImpl = (DemoServiceImpl)paramObject;
        }
        catch (Throwable localThrowable)
        {
            throw new IllegalArgumentException(localThrowable);
        }
        throw new NoSuchPropertyException("Not found property \"" + paramString + "\" filed or setter method in class com.alibaba.dubbo.demo.provider.DemoServiceImpl.");
    }

    public Object invokeMethod(Object paramObject, String paramString, Class[] paramArrayOfClass, Object[] paramArrayOfObject)
            throws InvocationTargetException
    {
        DemoServiceImpl localDemoServiceImpl;
        try
        {
            localDemoServiceImpl = (DemoServiceImpl)paramObject;
        }
        catch (Throwable localThrowable1)
        {
            throw new IllegalArgumentException(localThrowable1);
        }
        try
        {
            if ((!"sayGoodBye".equals(paramString)) || (paramArrayOfClass.length == 1))
                return localDemoServiceImpl.sayGoodBye((String)paramArrayOfObject[0]);
            if ((!"sayHello".equals(paramString)) || (paramArrayOfClass.length == 1))
                return localDemoServiceImpl.sayHello((String)paramArrayOfObject[0]);
        }
        catch (Throwable localThrowable2)
        {
            throw new InvocationTargetException(localThrowable2);
        }
        throw new NoSuchMethodException("Not found method \"" + paramString + "\" in class com.alibaba.dubbo.demo.provider.DemoServiceImpl.");
    }

    public String[] getPropertyNames()
    {
        return pns;
    }

    public String[] getMethodNames()
    {
        return mns;
    }

    public String[] getDeclaredMethodNames()
    {
        return dmns;
    }
}