package models;

public class CurrencyPair {
    public String symbol, currency_group, currency_base, currency_quote;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCurrency_group() {
        return currency_group;
    }

    public void setCurrency_group(String currency_group) {
        this.currency_group = currency_group;
    }

    public String getCurrency_base() {
        return currency_base;
    }

    public void setCurrency_base(String currency_base) {
        this.currency_base = currency_base;
    }

    public String getCurrency_quote() {
        return currency_quote;
    }

    public void setCurrency_quote(String currency_quote) {
        this.currency_quote = currency_quote;
    }
}
