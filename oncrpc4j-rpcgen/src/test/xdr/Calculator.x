const PLAIN_ZERO = 0;
const HEX_ZERO = 0x0;
const SMALL_CONST = 0xFF00;
const LARGE_CONST = 0xFFF000000000;
const HUGE_CONST  = 0xFFF000000000000000000;
const UNSIGNED_LONG_HEX_CONST = 0xFFFFFFFFFFFFFFFF;
const UNSIGNED_LONG_OCT_CONST = 01777777777777777777777;
const UNSIGNED_LONG_DEC_CONST = 18446744073709551615;
const UNSIGNED_INT_HEX_CONST = 0xFFFFFFFF;
const UNSIGNED_INT_OCT_CONST = 037777777777;
const UNSIGNED_INT_DEC_CONST = 4294967295;

struct CalculationResult {
    hyper  result;
    unsigned hyper startMillis;
    unsigned hyper finishMillis;
};

program CALCULATOR {
    version CALCULATORVERS {
        CalculationResult add(hyper, hyper) = 1;
        hyper addSimple(hyper, hyper) = 2;
    } = 1;
} = 117;