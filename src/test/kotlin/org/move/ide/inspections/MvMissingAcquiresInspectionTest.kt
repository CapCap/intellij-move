package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionTestBase

class MvMissingAcquiresInspectionTest: InspectionTestBase(MvMissingAcquiresInspection::class) {
    fun `test no error when acquired type cannot be inferred`() = checkErrors("""
    module 0x1::M {
        struct Loan has key {}
        fun main() {
            move_from/*caret*/(@0x1);
        }
    }    
    """)

    fun `test no error when acquired type is unresolved`() = checkErrors("""
    module 0x1::M {
        fun main() {
            move_from<Loan>/*caret*/(@0x1);
        }
    }    
    """)

    fun `test error with fix`() = checkFixByText("Add missing acquires", """
    module 0x1::M {
        struct Loan has key {}
        fun main() {
            <error descr="Function 'main' is not marked as 'acquires Loan'">move_from<Loan>/*caret*/(@0x1)</error>;
        }
    }    
    """, """
    module 0x1::M {
        struct Loan has key {}
        fun main() acquires Loan {
            move_from<Loan>/*caret*/(@0x1);
        }
    }    
    """)

    fun `test error with fix two params`() = checkFixByText("Add missing acquires", """
    module 0x1::M {
        struct Loan has key {}
        struct Deal has key {}
        fun call() acquires Deal, Loan {
            move_from<Loan>(@0x1);
            move_from<Deal>(@0x1);
        }
        fun main() {
            <error descr="Function 'main' is not marked as 'acquires Deal, Loan'">call/*caret*/()</error>;
        }
    }    
    """, """
    module 0x1::M {
        struct Loan has key {}
        struct Deal has key {}
        fun call() acquires Deal, Loan {
            move_from<Loan>(@0x1);
            move_from<Deal>(@0x1);
        }
        fun main() acquires Deal, Loan {
            call/*caret*/();
        }
    }    
    """)

    fun `test error with fix fully qualified type`() = checkFixByText("Add missing acquires", """
    module 0x1::M {
        struct Loan has key {}
        fun main() {
            <error descr="Function 'main' is not marked as 'acquires Loan'">move_from<0x1::M::Loan>/*caret*/(@0x1)</error>;
        }
    }
    """, """
    module 0x1::M {
        struct Loan has key {}
        fun main() acquires Loan {
            move_from<0x1::M::Loan>/*caret*/(@0x1);
        }
    }
    """)

    fun `test no error if acquires type from different module`() = checkErrors("""
    module 0x1::Loans {
        struct Loan has key {}
        fun call() acquires Loan {
            move_from<Loan>(@0x1);
        }
    }    
    module 0x1::M {
        fun main() {
            call();
        }
    }
    """)

    fun `test available on move_from with different acquires`() = checkFixByText("Add missing acquires", """
    module 0x1::M {
        struct Loan has key {}
        struct Deal has key {}
        fun main() acquires Deal {
            <error descr="Function 'main' is not marked as 'acquires Loan'">move_from<Loan>/*caret*/(@0x1)</error>;
        }
    }    
    """, """
    module 0x1::M {
        struct Loan has key {}
        struct Deal has key {}
        fun main() acquires Deal, Loan {
            move_from<Loan>/*caret*/(@0x1);
        }
    }    
    """)

    fun `test add acquires if present with generic`() = checkFixByText("Add missing acquires", """
    module 0x1::M {
        struct CapState<phantom Feature> has key {}
        fun main<Feature>() {
            <error descr="Function 'main' is not marked as 'acquires CapState'">move_from<CapState<Feature>>/*caret*/(@0x1)</error>;
        }  
    }    
    """, """
    module 0x1::M {
        struct CapState<phantom Feature> has key {}
        fun main<Feature>() acquires CapState {
            move_from<CapState<Feature>>/*caret*/(@0x1);
        }  
    }    
    """)

    fun `test not available transitively in script`() = checkErrors("""
    module 0x1::M {
        struct Loan {}
        public fun call() acquires Loan {}
    }    
    script {
        fun main() {
            0x1::M::call();
        }
    }
    """)

    fun `test function acquires two types one is missing`() = checkFixByText("Add missing acquires", """
    module 0x1::M {
        struct S {}
        struct R {}
        public fun call() acquires R, S {}
        fun main() acquires R {
            <error descr="Function 'main' is not marked as 'acquires S'">call/*caret*/()</error>;
        }
    }
    """, """
    module 0x1::M {
        struct S {}
        struct R {}
        public fun call() acquires R, S {}
        fun main() acquires R, S {
            call/*caret*/();
        }
    }
    """)

    fun `test missing acquires for borrow_global with dot expr`() = checkWarnings("""
module 0x1::main {
    struct StakePool has key {
        locked_until_secs: u64,
    }
    fun get_lockup_secs(pool_address: address): u64 {
        <error descr="Function 'get_lockup_secs' is not marked as 'acquires StakePool'">borrow_global<StakePool>(pool_address)</error>.locked_until_secs
    }
}        
    """)

    fun `test missing acquires inside assert macro`() = checkWarnings(
        """
module 0x1::main {
    struct EmergencyConfig has key { is_emergency: bool }
    fun is_emergency(): bool acquires EmergencyConfig {
        let config = borrow_global<EmergencyConfig>(@0x1);
        config.is_emergency
    }
    public fun assert_no_emergency() {
        assert!(!<error descr="Function 'assert_no_emergency' is not marked as 'acquires EmergencyConfig'">is_emergency()</error>, 1);
    }
}        
    """
    )

    fun `test inline functions do not need acquires`() = checkWarnings("""
module 0x1::main {
    inline fun borrow_object<T: key>(source_object: &Object<T>): &T {
        borrow_global<T>(object::object_address(source_object))
    }
}        
    """)

//    fun `test outer function requires acquires through inline function`() = checkWarnings("""
//module 0x1::main {
//    struct S has key {}
//    fun call() {
//        <error descr="Function 'call' is not marked as 'acquires S'">borrow_object<S>()</error>;
//    }
//    inline fun borrow_object<T: key>(source_object: &Object<T>): &T {
//        borrow_global<T>(object::object_address(source_object))
//    }
//}
//    """)
}
