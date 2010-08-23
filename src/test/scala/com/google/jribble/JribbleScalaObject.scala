/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.jribble

import com.google.jribble.ast._
import scala.util.parsing.input.CharSequenceReader
import org.junit.Assert._
import org.junit.Test

class JribbleScalaObject {
  val parsers = new Parsers {}

  implicit def liftParser[T](p: parsers.Parser[T]): String => T =
    (new CharSequenceReader(_: String)) andThen p andThen {
      _ match {
        case parsers.Success(result, _) => result
        case x => error("Could not parse the input because " + x)
      }
  }

  @Test
  def scalaObject {
    val input = """public abstract interface Lscala/ScalaObject; {
}
"""
    val output = parsers.interfaceDef(input)
    assertTrue {
      output match {
        case _: InterfaceDef => true
        case _ => false
      }
    }
  }
}