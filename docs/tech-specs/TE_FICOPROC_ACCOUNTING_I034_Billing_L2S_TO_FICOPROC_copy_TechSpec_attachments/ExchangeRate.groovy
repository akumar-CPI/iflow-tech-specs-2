
import com.sap.it.api.mapping.Output;

/**
 * UDF: getRatesForFundingCurrencies
 * Purpose:
 *   For each value in fundingCurrency queue, find the corresponding rate from
 *   currencies/rates reference queues and output one rate per funding currency.
 *
 * Parameters (configure in Graphical Mapping):
 *   - currencies      : String[] (Queue)  e.g., ["USD","EUR","INR"]
 *   - rates           : String[] (Queue)  e.g., ["1.00","0.92","83.15"]
 *   - fundingCurrency : String[] (Queue)  e.g., ["EUR","INR","USD"]
 *
 * Output:
 *   - Adds one rate per fundingCurrency occurrence, in the same order.
 *   - If not found, behavior depends on emitEmptyWhenNotFound flag:
 *       - true  -> adds "" (keeps alignment/length)
 *       - false -> adds nothing (suppresses that position)
 */
def void getRatesForFundingCurrencies(String[] currencies,
                                      String[] rates,
                                      String[] fundingCurrency,
                                      Output output) {

    // ---- Configurable behavior ----
    final boolean emitEmptyWhenNotFound = true  // set to false to suppress unmatched

    if (currencies == null || rates == null || fundingCurrency == null) {
        return
    }

    // Build a lookup map from currency (case-insensitive) to rate.
    // If duplicates exist, the FIRST occurrence wins. Change to "put" to override with last.
    Map<String, String> rateByCurrency = new HashMap<>()
    int n = Math.min(currencies.length, rates.length)
    for (int i = 0; i < n; i++) {
        String c = currencies[i] != null ? currencies[i].trim() : null
        if (c == null || c.isEmpty()) continue
        String key = c.toUpperCase()
        if (!rateByCurrency.containsKey(key)) {
            String r = rates[i] != null ? rates[i].trim() : ""
            rateByCurrency.put(key, r)
        }
    }

    // For each funding currency occurrence, output one value
    for (int j = 0; j < fundingCurrency.length; j++) {
        String fc = fundingCurrency[j] != null ? fundingCurrency[j].trim() : null
        if (fc == null || fc.isEmpty()) {
            if (emitEmptyWhenNotFound) output.addValue("")
            continue
        }
        String key = fc.toUpperCase()
        if (rateByCurrency.containsKey(key)) {
            output.addValue(rateByCurrency.get(key))
        } else {
            if (emitEmptyWhenNotFound) {
                output.addValue("")   // preserve positional alignment
            } // else suppress (no output for this funding currency)
        }
    }
}
