// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

type AnotherPerson record {|
    string name;
    int age;
|};

type Person1 record {|
    string name;
|};

function testMissingRequiredField1() {
    Person1 p = {name:"John"};
    AnotherPerson ap = p;
}

type Person2 record {|
    string name;
    int age?;
|};

function testMissingRequiredField2() {
    Person2 p = {name:"John"};
    AnotherPerson ap = p;
}

type AnotherPerson3 record {|
    string name;
    float weight?;
|};

// Tests assignment when the LHS type has optional fields which don't correspond to any fields in the RHS type.
function testClosedToClosedAssignment1() returns AnotherPerson3 {
    Person1 p = {name:"John Doe"};
    AnotherPerson3 ap = p;
    return ap;
}

function testClosedToClosedAssignment2() {
    Person1 p = {name:"John Doe"};
    AnotherPerson3 ap = p;
    ap.weight = 60.5;
}
