struct CalculationResult {
    hyper  result;
    unsigned hyper startMillis;
    unsigned hyper finishMillis;
};

program CALCULATOR {
    version CALCULATORVERS {
        CalculationResult add(hyper, hyper) = 1;
    } = 1;
} = 117;