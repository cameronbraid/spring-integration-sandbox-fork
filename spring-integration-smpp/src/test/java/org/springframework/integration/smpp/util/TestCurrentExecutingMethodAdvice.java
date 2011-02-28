package org.springframework.integration.smpp.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactoryBean;

public class TestCurrentExecutingMethodAdvice {
	static String currentMethodName() {
		return CurrentExecutingMethodHolder.getCurrentlyExecutingMethod().getName();
	}

	static public class TestClassWithAMethod {
		public TestClassWithAMethod() {
		}

		private Log l = LogFactory.getLog(getClass());

		public void testMe() throws Throwable {
			Assert.assertEquals("testMe", currentMethodName());
			l.debug("this is just evil. The currently executing method name is " + currentMethodName());
		}
	}

	@Test
	public void testLoggingTheCurrentlyExecutingMethodName() throws Throwable {
		ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
		proxyFactoryBean.setProxyTargetClass(true);
		proxyFactoryBean.addAdvice(new CurrentMethodExposingMethodInterceptor());
		proxyFactoryBean.setTarget(new TestClassWithAMethod());

		TestClassWithAMethod testClassWithAMethod = (TestClassWithAMethod) proxyFactoryBean.getObject();
		testClassWithAMethod.testMe();
	}
}
