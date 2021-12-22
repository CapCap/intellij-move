package org.move.lang.completion.names

import org.move.utils.tests.completion.CompletionTestCase

class FunctionsCompletionTest : CompletionTestCase() {
    fun `test function call zero args`() = doSingleCompletion(
        """
        module M {
            fun frobnicate() {}
            fun main() {
                frob/*caret*/
            }
        }
    """, """
        module M {
            fun frobnicate() {}
            fun main() {
                frobnicate()/*caret*/
            }
        }
    """
    )

    fun `test function call one arg`() = doSingleCompletion(
        """
        module M {
            fun frobnicate(a: u8) {}
            fun main() {
                frob/*caret*/
            }
        }
    """, """
        module M {
            fun frobnicate(a: u8) {}
            fun main() {
                frobnicate(/*caret*/)
            }
        }
    """
    )

    fun `test function call with parens`() = doSingleCompletion(
        """
        module M {
            fun frobnicate() {}
            fun main() {
                frob/*caret*/()
            }
        }
    """, """
        module M {
            fun frobnicate() {}
            fun main() {
                frobnicate()/*caret*/
            }
        }
    """
    )

    fun `test function call with parens with arg`() = doSingleCompletion(
        """
        module M {
            fun frobnicate(a: u8) {}
            fun main() {
                frob/*caret*/()
            }
        }
    """, """
        module M {
            fun frobnicate(a: u8) {}
            fun main() {
                frobnicate(/*caret*/)
            }
        }
    """
    )

//    fun `test complete function name in spec fun without parens`() = doSingleCompletion(
//        """
//        module M {
//            fun frobnicate(a: u8) {}
//            spec fun frob/*caret*/
//        }
//    """, """
//        module M {
//            fun frobnicate(a: u8) {}
//            spec fun frobnicate /*caret*/
//        }
//    """
//    )

//    fun `test spec fun do not add space if already there`() = doSingleCompletion(
//        """
//        module M {
//            fun frobnicate(a: u8) {}
//            spec fun frob/*caret*/ {}
//        }
//    """, """
//        module M {
//            fun frobnicate(a: u8) {}
//            spec fun frobnicate/*caret*/ {}
//        }
//    """
//    )

//    fun `test define function accessible before definition`() = doSingleCompletion(
//        """
//        module M {
//            spec module {
//                res/*caret*/;
//
//                define reserve_exists(): bool {
//                   exists<Reserve>(CoreAddresses::CURRENCY_INFO_ADDRESS())
//                }
//            }
//        }
//    """, """
//        module M {
//            spec module {
//                reserve_exists()/*caret*/;
//
//                define reserve_exists(): bool {
//                   exists<Reserve>(CoreAddresses::CURRENCY_INFO_ADDRESS())
//                }
//            }
//        }
//    """
//    )

//    fun `test generic function call with type parameters`() = doSingleCompletion("""
//        module M {
//            fun frobnicate<T>(a: T) {}
//            fun main() {
//                frob/*caret*/
//            }
//        }
//    """, """
//        module M {
//            fun frobnicate<T>(a: T) {}
//            fun main() {
//                frobnicate</*caret*/>()
//            }
//        }
//    """)

    fun `test type parameters accessible in types completion`() = doSingleCompletion(
        """
        module M {
            fun main<CoinType>() {
                let a: Coi/*caret*/
            }
        }
    """, """
        module M {
            fun main<CoinType>() {
                let a: CoinType/*caret*/
            }
        }
    """
    )

    fun `test no function completion in type position`() = checkNoCompletion(
        """
        address 0x1 {
        module Transaction {
            public fun create() {}
        }
        }
        
        module M {
            fun main(a: 0x1::Transaction::cr/*caret*/) {}
        }
    """
    )

    fun `test public and public(friend) completions for module`() = completionFixture.checkContainsCompletion(
        """
        address 0x1 {
        module Transaction {
            friend 0x1::M;
            public(friend) fun create_friend() {}
            public fun create() {}
        }
        }
        
        module 0x1::M {
            fun main() {
                0x1::Transaction::cr/*caret*/
            }
        }
    """, listOf("create", "create_friend"))

    fun `test public and public(script) completions for script`() = completionFixture.checkContainsCompletion(
        """
        address 0x1 {
        module Transaction {
            friend 0x1::M;
            public(script) fun create_script() {}
            public fun create() {}
        }
        }
        
        script {
            fun main() {
                0x1::Transaction::cr/*caret*/
            }
        }
    """, listOf("create", "create_script"))

    fun `test Self completion`() = completionFixture.checkContainsCompletion(
        """
        address 0x1 {
        module Transaction {
            friend 0x1::M;
            public(friend) fun create_friend() {}
            public(script) fun create_script() {}
            public fun create() {}
            fun create_private() {}
            
            fun main() {
                Self::/*caret*/
            }
        }
        }
    """, listOf("create", "create_script", "create_friend", "create_private"))

    fun `test fq completion for use`() = doSingleCompletion("""
    module 0x1::M1 {
        public fun call() {}
    }
    module 0x1::M2 {
        use 0x1::M1::c/*caret*/
    }
    """, """
    module 0x1::M1 {
        public fun call() {}
    }
    module 0x1::M2 {
        use 0x1::M1::call/*caret*/
    }
    """)

    fun `test fq completion for reference expr`() = doSingleCompletion("""
    module 0x1::M1 {
        public fun call() {}
    }
    module 0x1::M2 {
        fun m() {
            0x1::M1::c/*caret*/
        }
    }
    """, """
    module 0x1::M1 {
        public fun call() {}
    }
    module 0x1::M2 {
        fun m() {
            0x1::M1::call()/*caret*/
        }
    }
    """)
}
