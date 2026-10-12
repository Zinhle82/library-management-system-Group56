package com.pixelpioneers.library.tests;

/** Tiny test helper (no external libraries needed): counts passes/failures and prints one line per check. */
final class TestSupport {

    private static int passed = 0;
    private static int failed = 0;

    private TestSupport() { }

    static void check(String id, String description, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + id + "  " + description);
        } else {
            failed++;
            System.out.println("FAIL  " + id + "  " + description);
        }
    }

    /** Runs code that must throw an exception of the given type. */
    static void expectThrows(String id, String description, Class<? extends Exception> type, ThrowingRunnable code) {
        try {
            code.run();
            check(id, description + " (expected " + type.getSimpleName() + " but nothing was thrown)", false);
        } catch (Exception e) {
            check(id, description + " [" + e.getClass().getSimpleName() + "]", type.isInstance(e));
        }
    }

    interface ThrowingRunnable {
        void run() throws Exception;
    }

    static void section(String title) {
        System.out.println("\n== " + title + " ==");
    }

    /** Prints the totals and returns the process exit code (0 = all passed). */
    static int finish() {
        System.out.println("\n----------------------------------------");
        System.out.println("Passed: " + passed + "   Failed: " + failed + "   Total: " + (passed + failed));
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
        return failed == 0 ? 0 : 1;
    }
}
