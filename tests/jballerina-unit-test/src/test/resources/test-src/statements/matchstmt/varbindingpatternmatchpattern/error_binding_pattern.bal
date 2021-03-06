// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

const MSG = "Const Message";

function errorBindingPattern1(any|error e) returns string {
    match e {
        var error(_) => {
            return "match1";
        }
    }
    return "No match";
}

function testErrorBindingPattern1() {
    assertEquals("match1", errorBindingPattern1(error("Message")));
    assertEquals("match1", errorBindingPattern1(error(MSG)));
    assertEquals("No match", errorBindingPattern1("Message1"));
}

function errorBindingPattern2(any|error e) returns string {
    match e {
        var error(a) => {
            return <string> checkpanic a;
        }
    }
    return "No match";
}

function testErrorBindingPattern2() {
    assertEquals("Message", errorBindingPattern2(error("Message")));
    assertEquals(MSG, errorBindingPattern2(error(MSG)));
    assertEquals("No match", errorBindingPattern2("Message1"));
}

function errorBindingPattern3(error e) returns string {
    match e {
        var error(a) if a is string => {
            return a;
        }
    }
    return "No match";
}

function testErrorBindingPattern3() {
    assertEquals("Message", errorBindingPattern3(error("Message")));
}

function errorBindingPattern4(error e) returns string {
    match e {
        var error(a, error(b)) => {
            return "match1";
        }
        var error(a) => {
            return "match2";
        }
    }
    return "No match";
}

function testErrorBindingPattern4() {
    assertEquals("match1", errorBindingPattern4(error("Message1", error("Message2"))));
    assertEquals("match2", errorBindingPattern4(error("Message1")));
}

function errorBindingPattern5(error e) returns string {
    match e {
        var error(a, b) if b is error => {
            return b.message();
        }
    }
    return "No match";
}

function testErrorBindingPattern5() {
    assertEquals("Message2", errorBindingPattern5(error("Message1", error("Message2"))));
    assertEquals("No match", errorBindingPattern5(error("Message1")));
}

function errorBindingPattern6(error e) returns string {
    match e {
        var error(a, b) if b is error && b.message() == "Old Message" => {
            return "match";
        }
        var error(a, b) => {
            return (<error> b).message();
        }
    }
    return "No match";
}

function testErrorBindingPattern6() {
    assertEquals("Message2", errorBindingPattern6(error("Message1", error("Message2"))));
    assertEquals("match", errorBindingPattern6(error("Message1", error("Old Message"))));
}

function errorBindingPattern7(error e) returns string {
    match e {
        var error(a, error(b), c1=c) => {
            return "match1";
        }
    }
    return "No match";
}

function testErrorBindingPattern7() {
    assertEquals("match1", errorBindingPattern7(error("Message1", error("Message2"), c1=200)));
    assertEquals("match1", errorBindingPattern7(error("Message1", error("Message2"), c1=2)));
    assertEquals("match1", errorBindingPattern7(error("Message1", error("Message2"), c1=4)));
    assertEquals("No match", errorBindingPattern7(error("Message1")));
}

type MyError1 error<record { int x; }>;

function errorBindingPattern8(error e) returns string {
    match e {
        var error MyError1 (a) if a is string && a == "Message" => {
            return "match1";
        }
        var error MyError1 (a, x = b) => {
            return "match2";
        }
    }
    return "No match";
}

function testErrorBindingPattern8() {
    error e1 = error MyError1("Message", x = 2);
    assertEquals("match1", errorBindingPattern8(e1));
    assertEquals("match2", errorBindingPattern8(error MyError1("Message1", x = 2)));
    assertEquals("match2", errorBindingPattern8(error MyError1("Message1", x = 4)));
    assertEquals("No match", errorBindingPattern8(error("Message1", x = "5")));
}

function assertEquals(anydata expected, anydata actual) {
    if expected == actual {
        return;
    }
    panic error("expected '" + expected.toString() + "', found '" + actual.toString () + "'");
}
