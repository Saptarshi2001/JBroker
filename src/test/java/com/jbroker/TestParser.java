/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jbroker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class TestParser {


Map<Integer, Topic> mp;
List<Topic> lst;
ProtocolParser parser;

@BeforeEach
void setup() {
    mp = new ConcurrentHashMap<>();
    lst = new CopyOnWriteArrayList<>();
    parser = new ProtocolParser(mp, lst);
}

// -------- CONNECT --------
@Test
void testValidConnect() {
    assertTrue(parser.validate("Connect {}"));
}

@Test
void testInvalidConnect() {
    assertFalse(parser.validate("Connect"));
}

// -------- PUB --------
@Test
void testValidPub() {
    assertTrue(parser.validate("Pub foo 5"));
}

@Test
void testInvalidPub() {
    assertFalse(parser.validate("Pub foo"));
}

// -------- SUB --------
@Test
void testValidSub() {
    assertTrue(parser.validate("Sub foo 1"));
}

@Test
void testInvalidSub() {
    assertFalse(parser.validate("Sub"));
}

// -------- UNSUB --------
@Test
void testValidUnsub() {
    assertTrue(parser.validate("Unsub 1"));
}

@Test
void testInvalidUnsub() {
    assertFalse(parser.validate("Unsub"));
}


}
