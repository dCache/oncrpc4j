const PLAIN_ZERO = 0;
const HEX_ZERO = 0x0;
const SMALL_CONST = 0xFF00;
const LARGE_CONST = 0xFFF000000000;
const HUGE_CONST  = 0xFFF000000000000000000;

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