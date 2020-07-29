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

import ballerina/lang.array;
import ballerina/lang.test as test;

function testLength() returns int {
    int[] arr = [10, 20, 30, 40];
    return arr.length();
}

function testIterator() returns string {
    string[] arr = ["Hello", "World!", "From", "Ballerina"];
    abstract object {
         public function next() returns record {| string value; |}?;
    } itr = arr.iterator();

    record {| string value; |}|() elem = itr.next();
    string result = "";

    while (elem is record {| string value; |}) {
        result += elem.value;
        elem = itr.next();
    }

    return result;
}

function testEnumerate() {
    string[] arr = ["Hello", "World!", "From", "Ballerina"];
    [int, string][] enumerateArry =  arr.enumerate();
    test:assertEquals(enumerateArry[0][0], 0);
    test:assertEquals(enumerateArry[0][1], "Hello");
    test:assertEquals(enumerateArry[1][0], 1);
    test:assertEquals(enumerateArry[1][1], "World!");
    test:assertEquals(enumerateArry[2][0], 2);
    test:assertEquals(enumerateArry[2][1], "From");
    test:assertEquals(enumerateArry[3][0], 3);
    test:assertEquals(enumerateArry[3][1], "Ballerina");
}

function testMap() {
    int[] arr = [10, 20, 30, 40];
    int[] newArr = arr.'map(function (int x) returns int {
        return x/10;
    });
    test:assertEquals(newArr[0], 1);
    test:assertEquals(newArr[1], 2);
    test:assertEquals(newArr[2], 3);
    test:assertEquals(newArr[3], 4);
}

function testForeach() returns string {
    string?[] arr = ["Hello", "World!", (), "from", "Ballerina"];
    string result = "";

    arr.forEach(function (string? x) {
        if (x is string) {
            result += x;
        }
    });

    return result;
}

function testSlice() {
    float[] arr = [12.34, 23.45, 34.56, 45.67, 56.78];
    float[] r1 = arr.slice(1, 4);
    float[] r2 = arr.slice(2);
    float[] r3 = array:slice(arr, 3);
    test:assertEquals(r1.length(), 3);
    test:assertEquals(r1[0], 23.45);
    test:assertEquals(r1[1], 34.56);
    test:assertEquals(r1[2], 45.67);
    test:assertEquals(r2.length(), 3);
    test:assertEquals(r2[0], 34.56);
    test:assertEquals(r2[1], 45.67);
    test:assertEquals(r2[2], 56.78);
    test:assertEquals(r3.length(), 2);
    test:assertEquals(r3[0], 45.67);
    test:assertEquals(r3[1], 56.78);
}

function testPushAfterSlice() {
     float[] arr = [12.34, 23.45, 34.56, 45.67, 56.78];
     float[] s = arr.slice(1, 4);
     int sl = s.length();
     test:assertEquals(sl, 3);
     s.push(20.1);
     int slp = s.length();
     test:assertEquals(slp, 4);
     test:assertEquals(s[0], 23.45);
     test:assertEquals(s[1], 34.56);
     test:assertEquals(s[2], 45.67);
     test:assertEquals(s[3], 20.1);
}

function testPushAfterSliceFixed() {
     int[5] arr = [1, 2, 3, 4, 5];
     int[] s = arr.slice(3);
     int sl = s.length();
     test:assertEquals(sl, 2);
     s.push(88);
     int slp = s.length();
     test:assertEquals(slp, 3);
     test:assertEquals(s[0], 4);
     test:assertEquals(s[1], 5);
     test:assertEquals(s[2], 88);
}

function testSliceOnTupleWithRestDesc() {
    [int, string...] x = [1, "hello", "world"];
    (int|string)[] a = x.slice(1);
    test:assertEquals(2, a.length());
    test:assertEquals("hello", a[0]);
    test:assertEquals("world", a[1]);

    [int, int, boolean...] y = [1, 2, true, false, true];
    (int|boolean)[] b = y.slice(1, 4);
    test:assertEquals(3, b.length());
    test:assertEquals(2, b[0]);
    test:assertTrue(<boolean>b[1]);
    test:assertFalse(<boolean>b[2]);
}

function testRemove() {
    string[] arr = ["Foo", "Bar", "FooFoo", "BarBar"];
    string elem = arr.remove(2);
    test:assertEquals(elem, "FooFoo");
    test:assertEquals(arr.length(), 3);
    test:assertEquals(arr[0], "Foo");
    test:assertEquals(arr[1], "Bar");
    test:assertEquals(arr[2], "BarBar");
}

function testSort() {
    int[] arr = [98, 34, 44, 87, 13, 2, 1, 13];
    int[] sorted = arr.sort(function (int x, int y) returns int {
        return x - y;
    });
    test:assertEquals(sorted.length(), 8);
    test:assertEquals(sorted[0], 1);
    test:assertEquals(sorted[1], 2);
    test:assertEquals(sorted[2], 13);
    test:assertEquals(sorted[3], 13);
    test:assertEquals(sorted[4], 34);
    test:assertEquals(sorted[5], 44);
    test:assertEquals(sorted[6], 87);
    test:assertEquals(sorted[7], 98);

    test:assertSame(sorted, arr);
}

function testReduce() returns float {
    int[] arr = [12, 15, 7, 10, 25];
    float avg = arr.reduce(function (float accum, int val) returns float {
        return accum + <float>val / arr.length();
    }, 0.0);
    return avg;
}

type Grade "A+"|"A"|"A-"|"B+"|"B"|"B-"|"C"|"F";

function testIterableOpChain() returns float {
    [Grade, int][] grades = [["A+", 2], ["A-", 3], ["B", 3], ["C", 2]];

    int totalCredits = grades.reduce(function (int accum, [Grade, int] grade) returns int {
         return accum + grade[1];
     }, 0);

    float gpa = grades.'map(gradeToValue).reduce(function (float accum, [float, int] gradePoint) returns float {
        return accum + (gradePoint[0] * gradePoint[1]) / totalCredits;
    }, 0.0);

    return gpa;
}

function gradeToValue([Grade, int] grade) returns [float, int] {
    match grade[0] {
        "A+" => {return [4.2, grade[1]];}
        "A" => {return [4.0, grade[1]];}
        "A-" => {return [3.7, grade[1]];}
        "B+" => {return [3.3, grade[1]];}
        "B" => {return [3.0, grade[1]];}
        "B-" => {return [2.7, grade[1]];}
        "C" => {return [2.0, grade[1]];}
        "F" => {return [0.0, grade[1]];}
    }
    error e = error("Invalid grade: " + <string>grade[0]);
    panic e;
}

function testIndexOf() {
    anydata[] arr = [10, "foo", 12.34, true, <map<string>>{"k":"Bar"}];
    map<string> m = {"k":"Bar"};
    int? i1 = arr.indexOf(m);
    int? i2 = arr.indexOf(50);
    test:assertEquals(i1, 4);
    test:assertEquals(i2, ());
}

function testLastIndexOf() {
    anydata[] array = [10, 10, 10, "foo", "foo", "foo", 12.34, 12.34, true, true, <map<string>>{"k":"Bar"},
                       <map<string>>{"k":"Bar"}, [12, true], [12, true]];
    map<string> m1 = {"k":"Bar"};
    map<string> m2 = {"k":"Foo"};
    anydata[] arr1 = [12, true];
    anydata[] arr2 = [12, false];

    int? i1 = array.lastIndexOf(10);
    int? i2 = array.lastIndexOf("foo");
    int? i3 = array.lastIndexOf(12.34);
    int? i4 = array.lastIndexOf(true);
    int? i5 = array.lastIndexOf(m1);
    int? i6 = array.lastIndexOf(arr1);

    int? i7 = array.lastIndexOf(11);
    int? i8 = array.lastIndexOf("Bar");
    int? i9 = array.lastIndexOf(12.33);
    int? i10 = array.lastIndexOf(false);
    int? i11 = array.lastIndexOf(m2);
    int? i12 = array.lastIndexOf(arr2);

    if (<int>i1 != 2 && <int>i2 != 5 && <int>i3 != 7 && <int>i4 != 9 && <int>i5 != 11 && <int>i6 != 13 &&
                i7 != () && i8 != () && i9 != () && i10 != () && i11 != () && i12 != ()) {
        error err = error("'lastIndexOf' does not return correct value");
        panic err;
    }
}

function testReverse() {
    int[] arr = [10, 20, 30, 40, 50];
    int[] arrReverse = arr.reverse();
    test:assertEquals(arrReverse[0], 50);
    test:assertEquals(arrReverse[1], 40);
    test:assertEquals(arrReverse[2], 30);
    test:assertEquals(arrReverse[3], 20);
    test:assertEquals(arrReverse[4], 10);
}

type Person record {|
    int id;
    string name;
    int pilotingScore;
    int shootingScore;
    boolean isForceUser;
|};

// example from: https://medium.com/poka-techblog/simplify-your-javascript-use-map-reduce-and-filter-bd02c593cc2d
function testIterableOpChain2() returns int {
    Person[] personnel = [
      {
        id: 5,
        name: "Luke Skywalker",
        pilotingScore: 98,
        shootingScore: 56,
        isForceUser: true
      },
      {
        id: 82,
        name: "Sabine Wren",
        pilotingScore: 73,
        shootingScore: 99,
        isForceUser: false
      },
      {
        id: 22,
        name: "Zeb Orellios",
        pilotingScore: 20,
        shootingScore: 59,
        isForceUser: false
      },
      {
        id: 15,
        name: "Ezra Bridger",
        pilotingScore: 43,
        shootingScore: 67,
        isForceUser: true
      },
      {
        id: 11,
        name: "Caleb Dume",
        pilotingScore: 71,
        shootingScore: 85,
        isForceUser: true
      }
    ];

    int totalJediScore = personnel.filter(function (Person p) returns boolean {
        return p.isForceUser;
    }).map(function (Person jedi) returns int {
        return jedi.pilotingScore + jedi.shootingScore;
    }).reduce(function (int accum, int val) returns int {
        return accum + val;
    }, 0);

    return totalJediScore;
}

function testForEach() returns string {
    string[] days = ["Sun", "Mon", "Tues"];
    string result = "";

    foreach var day in days {
        result += day;
    }

    return result;
}

function testSetLength(int newLength) returns [int, int[], int[]] {
    int[] ar = [1, 2, 3, 4, 5, 6, 7];
    ar.setLength(newLength);
    int [] ar2 = ar.clone();
    ar2.setLength(newLength+1);
    return [ar.length(), ar, ar2];
}

function testSetLengthDataProvider() {
    [int, int, int[], int[]][] a = [[0, 0, [], [0]], [1, 1, [1], [1, 0]], [6, 6, [1, 2, 3, 4, 5, 6], [1, 2, 3, 4, 5, 6, 0]],
     [7, 7, [1, 2, 3, 4, 5, 6, 7], [1, 2, 3, 4, 5, 6, 7, 0]], [8, 8, [1, 2, 3, 4, 5, 6, 7, 0], [1, 2, 3, 4, 5, 6, 7, 0, 0]]];
    foreach [int, int, int[], int[]] element in a {
        [int, int[], int[]] results = testSetLength(element[0]);
        test:assertEquals(results[0], element[1]);
        test:assertEquals(results[1], element[2]);
        test:assertEquals(results[2], element[3]);
    }
}

function testShift() {
    int[] s = [1, 2, 3, 4, 5];
    var e = s.shift();
    test:assertEquals(s, [2, 3, 4, 5]);
    test:assertEquals(e, 1);
}

function testUnshift() {
    int[] s = [1, 2, 3, 4, 5];
    s.unshift(8, 8);
    test:assertEquals(s, [8, 8, 1, 2, 3, 4, 5]);
}

type Obj object {
    int i;
    int j;
    function init(int i, int j) {
        self.i = i;
        self.j = j;
    }
};

function testUnshiftTypeWithoutFillerValues() {
    Obj[] arr = [];
    arr.unshift(new Obj(1, 1), new Obj(1,2));
    test:assertEquals(arr.length(), 2);
}

function testRemoveAll() {
    int[] ar = [1, 2, 3, 4, 5, 6, 7];
    ar.removeAll();
    test:assertEquals(ar, []);
}

function testRemoveAllFixedLengthArray() returns int[] {
    int[7] ar = [1, 2, 3, 4, 5, 6, 7];
    ar.removeAll();
    return ar;
}

function testTupleResize() returns [int, string] {
    [int, string] t = [1, "hello"];
    t.setLength(3);
    return t;
}

function testTupleRemoveAll() returns [int, string] {
    [int, string] t = [1, "hello"];
    t.removeAll();
    return t;
}

function testTupleRemoveAllForTupleWithRestMemberType() returns [int, string] {
    [int, string, boolean...] t = [1, "hello", true];
    t.removeAll();
    return t;
}

function testTupleRemoveAllForTupleWithJustRestMemberType() returns boolean {
    [int...] t = [1, 2, 3];
    t.removeAll();
     return t.length() == 0;
}

function testTupleSetLengthLegal() returns boolean{
    [int, int, int...] t = [1, 2, 3, 4];
    t.setLength(2);
    return t.length() == 2;
}

function testTupleSetLengthIllegal() returns boolean {
    [int, int, int...] t = [1, 2, 3, 4];
    t.setLength(1);
    return t.length() == 1;
}

function testTupleSetLengthToSameAsOriginal() returns boolean{
    [int, int] t = [1, 2];
    t.setLength(2);
    return t.length() == 2;
}

function testSort2() {
    int[] arr = [618917, 342612, 134235, 330412, 361634, 106132, 664844, 572601, 898935, 752462, 422849, 967630,
    261402, 947587, 818112, 225958, 625762, 979376, -374104, 194169, 306130, 930271, 579739, 4141, 391419, 529224,
    92583, 709992, 481213, 851703, 152557, 995605, 88360, 595013, 526619, 497868, -246544, 17351, 601903, 634524,
    959892, 569029, 924409, 735469, -561796, 548484, 741307, 451201, 309875, 229568, 808232, 420862, 729149, 958388,
    228636, 834740, -147418, 756897, 872064, 670287, 487870, 984526, 352034, 868342, 705354, 21468, 101992, 716704,
    842303, 463375, 796488, -45917, 74477, 111826, 205038, 267499, 381564, 311396, 627858, 898090, 66917, 119980,
    601003, 962077, 757150, 636247, 965398, 993533, 780387, 797889, 384359, -80982, 817361, 117263, 819125, 162680,
    374341, 297625, 89008, 564847];

    int[] sorted = arr.sort(function (int x, int y) returns int {
        return x - y;
    });
    int sl = sorted.length();
    test:assertEquals(sl, 100);
    foreach int i in 1 ... (sl-1) {
           test:assertTrue(sorted[i] > sorted[i - 1]);
    }
}

function testBooleanPush() {
    boolean[] arr = [false, true];
    boolean b = false;

    boolean[] moreBooleans = [true, true];

    arr.push(b);
    arr.push(false, false);
    arr.push(...moreBooleans);

    array:push(arr, true);
    array:push(arr, true, false);
    array:push(arr, ...moreBooleans);

    test:assertEquals(12, arr.length());

    test:assertFalse(arr[0]);
    test:assertTrue(arr[1]);
    test:assertFalse(arr[2]);
    test:assertFalse(arr[3]);
    test:assertFalse(arr[4]);
    test:assertTrue(arr[5]);
    test:assertTrue(arr[6]);
    test:assertTrue(arr[7]);
    test:assertTrue(arr[8]);
    test:assertFalse(arr[9]);
    test:assertTrue(arr[10]);
    test:assertTrue(arr[11]);
}

function testBytePush() {
    byte[] arr = [1, 2];
    byte b = 3;

    byte[] moreBytes = [0, 3, 254];

    arr.push(b);
    arr.push(255, 254);
    arr.push(...moreBytes);

    array:push(arr, 65);
    array:push(arr, 66, 67);
    array:push(arr, ...moreBytes);

    test:assertEquals(14, arr.length());

    test:assertEquals(1, arr[0]);
    test:assertEquals(2, arr[1]);
    test:assertEquals(3, arr[2]);
    test:assertEquals(<byte> 255, arr[3]);
    test:assertEquals(254, arr[4]);
    test:assertEquals(0, arr[5]);
    test:assertEquals(3, arr[6]);
    test:assertEquals(254, arr[7]);
    test:assertEquals(65, arr[8]);
    test:assertEquals(66, arr[9]);
    test:assertEquals(67, arr[10]);
    test:assertEquals(0, arr[11]);
    test:assertEquals(3, arr[12]);
    test:assertEquals(254, arr[13]);
}

function testUnionArrayPush() {
    (Foo|Bar)[] arr = [{s: "a"}];

    arr.push({s: "b"}, {i: 1});

    (Foo|Bar)[] more = [{i: 2}, {s: "c"}];
    array:push(arr, ...more);

    test:assertEquals(5, arr.length());

    Foo|Bar val = arr[0];
    test:assertTrue(val is Foo && val.s == "a");

    val = arr[1];
    test:assertTrue(val is Foo && val.s == "b");

    val = arr[2];
    test:assertTrue(val is Bar && val.i == 1);

    val = arr[3];
    test:assertTrue(val is Bar && val.i == 2);

    val = arr[4];
    test:assertTrue(val is Foo && val.s == "c");
}

type Foo record {
    string s;
};

type Bar record{
    int i;
};

function testPushOnUnionOfSameBasicType() {
    int[2]|int[] arr = [1, 7, 3];
    arr.push(99);
    'array:push(arr, 100, 101);

    int[] moreInts = [999, 998];
    arr.push(...moreInts);
    'array:push(arr, ...moreInts);

    test:assertEquals(10, arr.length());

    test:assertEquals(1, arr[0]);
    test:assertEquals(7, arr[1]);
    test:assertEquals(3, arr[2]);
    test:assertEquals(99, arr[3]);
    test:assertEquals(100, arr[4]);
    test:assertEquals(101, arr[5]);
    test:assertEquals(999, arr[6]);
    test:assertEquals(998, arr[7]);
    test:assertEquals(999, arr[8]);
    test:assertEquals(998, arr[9]);
}

function testInvalidPushOnUnionOfSameBasicType() {
    int[]|string[] arr = [1, 2];

    var fn = function () {
        arr.push("foo");
    };

    error? res = trap fn();
    test:assertTrue(res is error);

    error err = <error> res;
    test:assertEquals("{ballerina/lang.array}InherentTypeViolation", err.message());
    test:assertEquals("incompatible types: expected 'int', found 'string'", err.detail()["message"].toString());

    fn = function () {
        arr.unshift("foo");
    };

    res = trap fn();
    test:assertTrue(res is error);

    err = <error> res;
    test:assertEquals("{ballerina/lang.array}InherentTypeViolation", err.message());
    test:assertEquals("incompatible types: expected 'int', found 'string'", err.detail()["message"].toString());
}

function testShiftOnTupleWithoutValuesForRestParameter() {
    [int, int...] intTupleWithRest = [0];

    var fn = function () {
        var x = intTupleWithRest.shift();
    };

    error? res = trap fn();
    test:assertTrue(res is error);

    error err = <error> res;
    test:assertEquals("{ballerina/lang.array}OperationNotSupported", err.message());
    test:assertEquals("shift() not supported on type 'null'", err.detail()["message"].toString());
}

function testAsyncFpArgsWithArrays() {
    int[] numbers = [-7, 2, -12, 4, 1];
    int count = 0;
    int[] filter = numbers.filter(function (int i) returns boolean {
        future<int> f1 = start getRandomNumber(i);
        int a = wait f1;
        return a >= 0;
    });
    filter.forEach(function (int i) {
        future<int> f1 = start getRandomNumber(i);
        int a = wait f1;
        filter[count] = i + 2;
        count = count + 1;
    });
    int reduce = filter.reduce(function (int total, int i) returns int {
        future<int> f1 = start getRandomNumber(i);
        int a = wait f1;
        return total + a;
    }, 0);
    test:assertEquals(reduce, 19);
    test:assertEquals(filter[0], 4);
    test:assertEquals(filter[1], 6);
    test:assertEquals(filter[2], 3);
}

function getRandomNumber(int i) returns int {
    return i + 2;
}
