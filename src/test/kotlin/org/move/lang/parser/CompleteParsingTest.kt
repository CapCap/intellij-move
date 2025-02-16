package org.move.lang.parser

import org.move.utils.tests.parser.MvParsingTestCase

class CompleteParsingTest : MvParsingTestCase("complete") {
    fun `test comments`() = doTest()
    fun `test addresses`() = doTest()
    fun `test attributes`() = doTest()

    // functions
    fun `test function declarations`() = doTest()
    fun `test function calls`() = doTest()

    // expressions
    fun `test strings`() = doTest()
    fun `test vectors`() = doTest()
    fun `test expressions`() = doTest()
    fun `test expressions assignments`() = doTest()
    fun `test expressions if else as`() = doTest()
    fun `test expressions angle brackets`() = doTest()
    fun `test expressions specs`() = doTest()

    // use
    fun `test use`() = doTest()
    fun `test friend`() = doTest()

    // assignments
    fun `test let patterns`() = doTest()
    fun `test assignments`() = doTest()

    // structs
    fun `test struct declarations`() = doTest()
    fun `test struct literals`() = doTest()

    // misc
    fun `test while loop inline assignment`() = doTest()
    fun `test contextual token operators`() = doTest()
    fun `test generics`() = doTest()
    fun `test annotated literals`() = doTest()

    fun `test macros`() = doTest()

    fun doTest() {
        super.doTest(true, true)
    }
}
