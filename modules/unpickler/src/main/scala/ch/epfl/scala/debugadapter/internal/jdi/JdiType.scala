package ch.epfl.scala.debugadapter.internal.jdi

import ch.epfl.scala.debugadapter.internal.binary.*
import java.lang.reflect.InvocationTargetException
import scala.jdk.CollectionConverters.*

class JdiType(obj: Any, className: String = "com.sun.jdi.Type") extends JavaReflection(obj, className) with Type:
  override def name: String = invokeMethod("name")
  def getSuperclass = ???
  def getImplementedInterfaces = ???