/*
 * Copyright 2008 Vitor Costa
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

package com.google.code.jdde;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.google.code.jdde.transaction.ConnectTest;
import com.google.code.jdde.transaction.ExecuteTest;
import com.google.code.jdde.transaction.PokeTest;
import com.google.code.jdde.transaction.RequestTest;

/**
 * 
 * @author Vitor Costa
 */
@RunWith(Suite.class)
@SuiteClasses({
	ConnectTest.class,
	ExecuteTest.class,
	PokeTest.class,
	RequestTest.class
})
public class JavaDdeTestSuite {

}
