package com.aibankingagent.ai_banking_agent.tool;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Computes the equated monthly instalment (EMI) for a loan.
 *
 * <p>Formula: EMI = P * r * (1+r)^n / ((1+r)^n - 1)
 *
 * <ul>
 *   <li>{@code P} = principal in BDT</li>
 *   <li>{@code rate_percent} = annual interest rate, e.g. 9.5 for 9.5%</li>
 *   <li>{@code tenure_months} = repayment period in months</li>
 *   <li>{@code r} = monthly rate = rate_percent / 12 / 100</li>
 *   <li>{@code n} = tenure_months</li>
 * </ul>
 *
 * <p>Returns the monthly EMI plus total interest, total payable, and
 * the inputs the calculation was based on (handy for the model's final
 * answer).
 */
@Component
public class CalculateEmiTool implements Tool {

    @Override
    public String name() {
        return "calculate_emi";
    }

    @Override
    public String description() {
        return "Calculate the equated monthly instalment (EMI) for a loan given principal, annual interest rate (percent), and tenure in months. Returns the monthly EMI, total interest, and total payable.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "principal", Map.of(
                                "type", "number",
                                "description", "Loan principal in BDT."),
                        "rate_percent", Map.of(
                                "type", "number",
                                "description", "Annual interest rate in percent (e.g. 9.5 means 9.5%)."),
                        "tenure_months", Map.of(
                                "type", "integer",
                                "description", "Repayment period in months.")),
                "required", java.util.List.of("principal", "rate_percent", "tenure_months"));
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        if (arguments == null) {
            return "Error: arguments were null.";
        }
        Double principal = numberArg(arguments, "principal");
        Double ratePercent = numberArg(arguments, "rate_percent");
        Integer tenure = intArg(arguments);

        if (principal == null || ratePercent == null || tenure == null) {
            return "Error: 'principal', 'rate_percent' and 'tenure_months' are all required.";
        }
        if (principal <= 0 || ratePercent < 0 || tenure <= 0) {
            return "Error: principal and tenure must be positive; rate must be non-negative.";
        }

        double monthlyRate = ratePercent / 12.0 / 100.0;
        int n = tenure;
        double emi;

        if (monthlyRate == 0.0) {
            // Edge case: 0% interest — straight division.
            emi = principal / n;
        } else {
            double factor = Math.pow(1 + monthlyRate, n);
            emi = principal * monthlyRate * factor / (factor - 1);
        }

        double totalPayable = emi * n;
        double totalInterest = totalPayable - principal;

        return String.format(
                "Principal: %.2f BDT%nAnnual rate: %.3f%%%nTenure: %d months%nMonthly EMI: %.2f BDT%nTotal interest: %.2f BDT%nTotal payable: %.2f BDT",
                principal, ratePercent, n, emi, totalInterest, totalPayable);
    }

    private static Double numberArg(Map<String, Object> args, String key) {
        Object raw = args.get(key);
        if (raw == null) return null;
        if (raw instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer intArg(Map<String, Object> args) {
        Object raw = args.get("tenure_months");
        if (raw == null) return null;
        if (raw instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
