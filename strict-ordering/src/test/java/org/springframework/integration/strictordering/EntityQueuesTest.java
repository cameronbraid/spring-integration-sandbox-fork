package org.springframework.integration.strictordering;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.strictordering.EntityQueues;


public class EntityQueuesTest {
  private EntityQueues<String,String> entityQueues;
  @Before
  public void setUp(){
	  entityQueues = new EntityQueues<String,String> ();
  }
  
  @Test
  public void testEmptyQueue(){
	  assertNull(entityQueues.remove("foo"));
	  assertEquals(0, entityQueues.size("foo"));
  }
  
  @Test
  public void testBasic(){
	  entityQueues.add("foo", "foo-1");
	  entityQueues.add("foo", "foo-2");
	  assertEquals(2,entityQueues.size("foo"));
	  assertEquals("foo-1",entityQueues.remove("foo"));
	  assertEquals("foo-2",entityQueues.remove("foo"));  
  }
  
  @Test
  public void testMultipleQueues(){
	  entityQueues.add ("foo", "foo-1");
	  entityQueues.add ("bar", "bar-1");
	  entityQueues.add("foo", "foo-2");
	  entityQueues.add("bar", "bar-2");
	  entityQueues.add("bar", "bar-3");
	  entityQueues.add("baz", "baz-1");
	  entityQueues.add("baz", "baz-2");
	  
	  assertEquals(2,entityQueues.size("foo"));
	  assertEquals(3,entityQueues.size("bar"));
	  assertEquals(2,entityQueues.size("baz"));
	  
	  assertEquals(3,entityQueues.keySet().size());
	 
	  assertTrue (entityQueues.keySet().contains("foo"));
	  assertTrue (entityQueues.keySet().contains("bar"));
	  assertTrue (entityQueues.keySet().contains("baz"));
	  
	  assertEquals("foo-1",entityQueues.remove("foo"));
	  assertEquals("bar-1",entityQueues.remove("bar"));
	  assertEquals("baz-1",entityQueues.remove("baz"));
	  
	  assertEquals("foo-2",entityQueues.remove("foo"));
	  assertEquals("bar-2",entityQueues.remove("bar"));
	  assertEquals("baz-2",entityQueues.remove("baz"));
	  
	  assertEquals("bar-3",entityQueues.remove("bar"));
	  
	  assertNull(entityQueues.remove("foo"));
	  assertNull(entityQueues.remove("baz"));
	  
	  assertEquals(0,entityQueues.keySet().size());
	  assertEquals(0,entityQueues.size("baz"));
  }
}
