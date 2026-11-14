package com.highstream.proxy;

import com.highstream.user.demo.JdkProxyFactory;
import com.highstream.user.demo.SmsService;
import com.highstream.user.demo.SmsServiceImp;
import org.junit.jupiter.api.Test;

public class ProxyTests {

    @Test
    public void test() {


        SmsService smsService = (SmsService) JdkProxyFactory.getProxy(new SmsServiceImp());
        smsService.send("abc");
         int COUNT_BITS = Integer.SIZE - 3;
         int COUNT_MASK = (1 << COUNT_BITS) - 1;
         int RUNNING    = -1 << COUNT_BITS;
         int SHUTDOWN   =  0 << COUNT_BITS;
         int STOP       =  1 << COUNT_BITS;
         int TIDYING    =  2 << COUNT_BITS;
         int TERMINATED =  3 << COUNT_BITS;
        System.out.println();
         System.out.println("COUNT_BIT  " + COUNT_BITS);
         System.out.println("COUNT_MASK " + COUNT_MASK);
         System.out.println("RUNNING    " + RUNNING   );
        System.out.println("SHUTDOWN   " + SHUTDOWN  );
         System.out.println("STOP       " + STOP      );
         System.out.println("TIDYING    " + TIDYING   );
         System.out.println("TERMINATE  " + TERMINATED);

        System.out.println("Integer.SIZE : " + String.valueOf( Integer.SIZE - 3));

    }
}
