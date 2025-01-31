package com.nectum.tradingv25.indicator.ta4j;

import com.nectum.tradingv25.indicator.custom.BooleanToNumIndicator;
import com.nectum.tradingv25.indicator.custom.CompositeIndicator;
import com.nectum.tradingv25.indicator.custom.MaxHIndicator;
import com.nectum.tradingv25.indicator.custom.PriceEarningsRatioIndicator;
import com.nectum.tradingv25.indicator.helpers.DateTimeToNumIndicator;
import com.nectum.tradingv25.indicator.helpers.WrapperIndicator;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.adx.*;
import org.ta4j.core.indicators.aroon.*;
import org.ta4j.core.indicators.bollinger.*;
import org.ta4j.core.indicators.candles.*;
import org.ta4j.core.indicators.donchian.*;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.indicators.ichimoku.*;
import org.ta4j.core.indicators.keltner.*;
import org.ta4j.core.indicators.pivotpoints.*;
import org.ta4j.core.indicators.statistics.*;
import org.ta4j.core.indicators.supertrend.*;
import org.ta4j.core.indicators.trend.*;
import org.ta4j.core.indicators.volume.*;


import org.ta4j.core.num.Num;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class IndicatorFactory {

    @FunctionalInterface
    public interface IndicatorCreator extends BiFunction<BarSeries, Integer, Indicator<Num>> {}

    private static final Map<String, IndicatorCreator> INDICATOR_MAP = new HashMap<>();

    static {
        // Basic (OHLCV)
        INDICATOR_MAP.put("open: open", (series, p) -> new OpenPriceIndicator(series));
        INDICATOR_MAP.put("high: high", (series, p) -> new HighPriceIndicator(series));
        INDICATOR_MAP.put("low: low", (series, p) -> new LowPriceIndicator(series));
        INDICATOR_MAP.put("close: close", (series, p) -> new ClosePriceIndicator(series));
        INDICATOR_MAP.put("volume: volume", (series, p) -> new VolumeIndicator(series));


        INDICATOR_MAP.put("typical_price: typical_price", (series, p) -> new TypicalPriceIndicator(series));
        INDICATOR_MAP.put("median_price: median_price", (series, p) -> new MedianPriceIndicator(series));

        // Moving Averages
        INDICATOR_MAP.put("sma: sma", (series, p) ->
                new SMAIndicator(new ClosePriceIndicator(series), p > 14 ? p : 14)); // Valor predeterminado: 14
        INDICATOR_MAP.put("ema: ema", (series, p) ->
                new EMAIndicator(new ClosePriceIndicator(series), p > 14 ? p : 14)); // Valor predeterminado: 14



        INDICATOR_MAP.put("wma: wma", (series, p) -> new WMAIndicator(new ClosePriceIndicator(series), p));
        INDICATOR_MAP.put("dema: dema", (series, p) -> new DoubleEMAIndicator(new ClosePriceIndicator(series), p));
        INDICATOR_MAP.put("tema: tema", (series, p) -> new TripleEMAIndicator(new ClosePriceIndicator(series), p));
        INDICATOR_MAP.put("zlema: zlema", (series, p) -> new ZLEMAIndicator(new ClosePriceIndicator(series), p));
        INDICATOR_MAP.put("kama: kama", (series, p) -> new KAMAIndicator(new ClosePriceIndicator(series), 10, 2, 30));


        INDICATOR_MAP.put("rsi: rsi", (series, p) ->
                new RSIIndicator(new ClosePriceIndicator(series), p > 0 ? p : 14) // Si p <= 2, usa 14
        );

        /*
        Este indicador calcula el valor máximo del precio alto (High) dentro de un período especificado.
        Si el período es más largo que la cantidad de barras disponibles, tomará todas las barras disponibles.
        * */
        INDICATOR_MAP.put("maxh: maxh", (series, period) -> {
            // 1) Creamos la base: HighPriceIndicator
            Indicator<Num> highIndicator = new HighPriceIndicator(series);
            // 2) Lo envolvemos en nuestro MaxHIndicator
            return new MaxHIndicator(highIndicator);
        });



        INDICATOR_MAP.put("priceearningsratio", (series, p) ->
                new PriceEarningsRatioIndicator(
                        new ClosePriceIndicator(series),                // Indicador de precio
                        new ConstantIndicator<>(series, series.numOf(5)),
                        p > 0 ? p : 14
                )
        );


        INDICATOR_MAP.put("stochastic: stochastic_k", (series, p) -> new StochasticOscillatorKIndicator(series, p));
        INDICATOR_MAP.put("stochastic: stochastic_d", (series, p) -> new StochasticOscillatorDIndicator(new StochasticOscillatorKIndicator(series, p)));
        INDICATOR_MAP.put("macd: macd", (series, p) -> new MACDIndicator(new ClosePriceIndicator(series), p, p * 2));
        INDICATOR_MAP.put("cci: cci", (series, p) -> new CCIIndicator(series, p));
        INDICATOR_MAP.put("roc: roc", (series, p) -> new ROCIndicator(new ClosePriceIndicator(series), p));
        INDICATOR_MAP.put("stochasticrsi: stochasticrsi", (series, p) -> new StochasticRSIIndicator(new ClosePriceIndicator(series), p));
        INDICATOR_MAP.put("cmo: cmo", (series, p) -> new CMOIndicator(new ClosePriceIndicator(series), p));


        // ADX
        INDICATOR_MAP.put("adx: adx", (series, p) ->
                new ADXIndicator(series, p>14 ? p:14));
        INDICATOR_MAP.put("dx: dx", (series, p) -> new DXIndicator(series, p));
        INDICATOR_MAP.put("minusdi: minusdi", (series, p) -> new MinusDIIndicator(series, p));
        INDICATOR_MAP.put("minusdm: minusdm", (series, p) -> new MinusDMIndicator(series));
        INDICATOR_MAP.put("plusdi: plusdi", (series, p) -> new PlusDIIndicator(series, p));
        INDICATOR_MAP.put("plusdm: plusdm", (series, p) -> new PlusDMIndicator(series));

        INDICATOR_MAP.put("atr: atr", (series, p) -> new ATRIndicator(series, p));
        INDICATOR_MAP.put("ulcerindex: ulcerindex", (series, p) -> new UlcerIndexIndicator(new ClosePriceIndicator(series), p));

        // Trend
        INDICATOR_MAP.put("psar: psar", (series, p) -> new ParabolicSarIndicator(series));
        INDICATOR_MAP.put("awesome_oscillator: ao", (series, p) -> new AwesomeOscillatorIndicator(series));

        // Aroon
        INDICATOR_MAP.put("aroon: up", (series, p) -> new AroonUpIndicator(series, p));
        INDICATOR_MAP.put("aroon: down", (series, p) -> new AroonDownIndicator(series, p));
        INDICATOR_MAP.put("aroon: facade", (series, period) -> {
            AroonFacade aroonFacade = new AroonFacade(series, period);
            return new CompositeIndicator<>(Map.of(
                    "up", aroonFacade.up(),
                    "down", aroonFacade.down(),
                    "oscillator", aroonFacade.oscillator()
            ));
        });
        INDICATOR_MAP.put("aroon: oscillator", (series, period) -> new AroonOscillatorIndicator(series, period));

        // Bollinger
        INDICATOR_MAP.put("bollinger: middle", (series, p) -> new BollingerBandsMiddleIndicator(new SMAIndicator(new ClosePriceIndicator(series), p)));
        INDICATOR_MAP.put("bollinger: facade", (series, period) -> {
            BollingerBandFacade facade = new BollingerBandFacade(series, period, 2);
            return new CompositeIndicator<>(Map.of(
                    "middle", facade.middle(),
                    "upper", facade.upper(),
                    "lower", facade.lower(),
                    "bandwidth", facade.bandwidth(),
                    "percentB", facade.percentB()
            ));
        });
        INDICATOR_MAP.put("bollinger: width", (series, period) -> {
            BollingerBandsMiddleIndicator middle = new BollingerBandsMiddleIndicator(new SMAIndicator(new ClosePriceIndicator(series), period));
            BollingerBandsUpperIndicator upper = new BollingerBandsUpperIndicator(middle, new StandardDeviationIndicator(new ClosePriceIndicator(series), period));
            BollingerBandsLowerIndicator lower = new BollingerBandsLowerIndicator(middle, new StandardDeviationIndicator(new ClosePriceIndicator(series), period));
            return new BollingerBandWidthIndicator(upper, middle, lower);
        });
        INDICATOR_MAP.put("bollinger: upper", (series, p) -> {
            BollingerBandsMiddleIndicator middle = new BollingerBandsMiddleIndicator(new SMAIndicator(new ClosePriceIndicator(series), p));
            StandardDeviationIndicator stdDev = new StandardDeviationIndicator(new ClosePriceIndicator(series), p);
            return new BollingerBandsUpperIndicator(middle, stdDev);
        });
        INDICATOR_MAP.put("bollinger: lower", (series, p) -> {
            BollingerBandsMiddleIndicator middle = new BollingerBandsMiddleIndicator(new SMAIndicator(new ClosePriceIndicator(series), p));
            StandardDeviationIndicator stdDev = new StandardDeviationIndicator(new ClosePriceIndicator(series), p);
            return new BollingerBandsLowerIndicator(middle, stdDev);
        });
        INDICATOR_MAP.put("bollinger: percent_b", (series, p) -> {
            BollingerBandsMiddleIndicator middle = new BollingerBandsMiddleIndicator(new SMAIndicator(new ClosePriceIndicator(series), p));
            StandardDeviationIndicator stdDev = new StandardDeviationIndicator(new ClosePriceIndicator(series), p);
            BollingerBandsUpperIndicator upper = new BollingerBandsUpperIndicator(middle, stdDev);
            BollingerBandsLowerIndicator lower = new BollingerBandsLowerIndicator(middle, stdDev);
            return new PercentBIndicator(new ClosePriceIndicator(series), p, 2.0);
        });



        // Candles
        INDICATOR_MAP.put("candles: bearish_engulfing",
                (series, p) -> new BooleanToNumIndicator(new BearishEngulfingIndicator(series)));

        INDICATOR_MAP.put("candles: lower_shadow",
                (series, p) -> new LowerShadowIndicator(series));

        INDICATOR_MAP.put("candles: real_body",
                (series, p) -> new RealBodyIndicator(series));

        INDICATOR_MAP.put("candles: upper_shadow",
                (series, p) -> new UpperShadowIndicator(series));

        INDICATOR_MAP.put("candles: bearish_harami",
                (series, p) -> new BooleanToNumIndicator(new BearishHaramiIndicator(series)));

        INDICATOR_MAP.put("candles: bullish_engulfing",
                (series, p) -> new BooleanToNumIndicator(new BullishEngulfingIndicator(series)));

        INDICATOR_MAP.put("candles: bullish_harami",
                (series, p) -> new BooleanToNumIndicator(new BullishHaramiIndicator(series)));

        INDICATOR_MAP.put("candles: three_black_crows",
                (series, p) -> new BooleanToNumIndicator(new ThreeBlackCrowsIndicator(series, p, 2.0)));

        INDICATOR_MAP.put("candles: three_white_soldiers",
                (series, p) -> new BooleanToNumIndicator(new ThreeWhiteSoldiersIndicator(series, p, series.numOf(2.0))));

        INDICATOR_MAP.put("candles: shooting_star",
                (series, p) -> new BooleanToNumIndicator(new ShootingStarIndicator(series)));

        INDICATOR_MAP.put("candles: doji",
                (series, p) -> new BooleanToNumIndicator(new DojiIndicator(series, p, 0.03)));

// Donchian Channel Indicators
        INDICATOR_MAP.put("donchian: middle",
                (series, p) -> new DonchianChannelMiddleIndicator(series, p));

        INDICATOR_MAP.put("donchian: upper",
                (series, p) -> new DonchianChannelUpperIndicator(series, p));

        INDICATOR_MAP.put("donchian: lower",
                (series, p) -> new DonchianChannelLowerIndicator(series,p));

// Helpers
        INDICATOR_MAP.put("helpers: amount",(series, p) -> new AmountIndicator(series));

        INDICATOR_MAP.put("helpers: boolean_transform",(series, p) -> new WrapperIndicator<>(BooleanTransformIndicator.isPositive(new ClosePriceIndicator(series))));

        INDICATOR_MAP.put("helpers: close_location_value",(series, p) -> new CloseLocationValueIndicator(series));

        INDICATOR_MAP.put("helpers: close_price_difference",(series, p) -> new ClosePriceDifferenceIndicator(series));

        INDICATOR_MAP.put("helpers: close_price_ratio",(series, p) -> new ClosePriceRatioIndicator(series));

        INDICATOR_MAP.put("helpers: combine",(series, p) -> CombineIndicator.plus(new HighPriceIndicator(series), new LowPriceIndicator(series)));

        INDICATOR_MAP.put("helpers: constant",(series, p) -> new ConstantIndicator<>(series, series.numOf(p)));

        INDICATOR_MAP.put("helpers: convergence_divergence",
                (series, p) -> new WrapperIndicator<>(
                        new ConvergenceDivergenceIndicator(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), p),
                        p,
                        ConvergenceDivergenceIndicator.ConvergenceDivergenceType.positiveConvergent
                )));

        INDICATOR_MAP.put("helpers: cross",(series, p)
                -> new WrapperIndicator<>(new CrossIndicator(
                        new SMAIndicator(new ClosePriceIndicator(series), p),
                        new SMAIndicator(new ClosePriceIndicator(series), p * 2)
                )));

        INDICATOR_MAP.put("helpers: date_time",
                (series, p) -> new DateTimeToNumIndicator(series));

        // Helpers
        INDICATOR_MAP.put("helpers: difference_percentage",
                (series, p) -> new DifferencePercentageIndicator(new ClosePriceIndicator(series), series.numOf(p)));

        INDICATOR_MAP.put("helpers: fixed_boolean",
                (series, p) -> new WrapperIndicator<>(new FixedBooleanIndicator(series, true, false, true)));


        INDICATOR_MAP.put("helpers: fixed_decimal",
                (series, p) -> new FixedDecimalIndicator(series, 1.0, 2.5, 3.3)); // Ejemplo: valores constantes numéricos

        INDICATOR_MAP.put("helpers: fixed",
                (series, p) -> new FixedIndicator(series, 42.0, 43.0));


        INDICATOR_MAP.put("helpers: gain",
                (series, p) -> new GainIndicator(new ClosePriceIndicator(series)));

        INDICATOR_MAP.put("helpers: highest_value",
                (series, p) -> new HighestValueIndicator(new ClosePriceIndicator(series), p));

        INDICATOR_MAP.put("helpers: loss",
                (series, p) -> new LossIndicator(new ClosePriceIndicator(series)));

        INDICATOR_MAP.put("helpers: lowest_value",
                (series, p) -> new LowestValueIndicator(new ClosePriceIndicator(series), p));

        INDICATOR_MAP.put("helpers: num",
                (series, p) -> new NumIndicator(series, bar -> series.numOf(bar.getClosePrice().doubleValue())));

        INDICATOR_MAP.put("helpers: previous_value",
                (series, p) -> new PreviousValueIndicator(new ClosePriceIndicator(series), p));

        // Helpers
        INDICATOR_MAP.put("helpers: running_total",
                (series, p) -> new RunningTotalIndicator(new ClosePriceIndicator(series), p));

        INDICATOR_MAP.put("helpers: sum",
                (series, p) -> new SumIndicator(new ClosePriceIndicator(series), new VolumeIndicator(series)));

        INDICATOR_MAP.put("helpers: trade_count",
                (series, p) -> new WrapperIndicator<>(new TradeCountIndicator(series)));

        INDICATOR_MAP.put("helpers: transform",
                (series, p) -> TransformIndicator.multiply(new ClosePriceIndicator(series), p));

        INDICATOR_MAP.put("helpers: tr",
                (series, p) -> new TRIndicator(series));

        INDICATOR_MAP.put("helpers: unstable",
                (series, p) -> new UnstableIndicator(new ClosePriceIndicator(series), p));

        // Ichimoku
        INDICATOR_MAP.put("ichimoku: chikou_span",
                (series, p) -> new IchimokuChikouSpanIndicator(series, p)); // `p` como retraso (usualmente 26)

        INDICATOR_MAP.put("ichimoku: kijun_sen",
                (series, p) -> new IchimokuKijunSenIndicator(series, p)); // Línea base (usualmente 26)

        INDICATOR_MAP.put("ichimoku: line",
                (series, p) -> new IchimokuLineIndicator(series, p)); // Línea genérica con `p` como período

        INDICATOR_MAP.put("ichimoku: senkou_span_a",
                (series, p) -> new IchimokuSenkouSpanAIndicator(series, 9, 26)); // Líneas estándar de Ichimoku

        INDICATOR_MAP.put("ichimoku: senkou_span_b",
                (series, p) -> new IchimokuSenkouSpanBIndicator(series, p, 26)); // Span B con desplazamiento (usualmente 26)

        INDICATOR_MAP.put("ichimoku: tenkan_sen",
                (series, p) -> new IchimokuTenkanSenIndicator(series, p)); // Línea de conversión (usualmente 9)

        // Keltner Channel
        INDICATOR_MAP.put("keltner: middle",
                (series, p) -> new KeltnerChannelMiddleIndicator(series, p));

        INDICATOR_MAP.put("keltner: upper",(series, p) -> new KeltnerChannelUpperIndicator(
                        new KeltnerChannelMiddleIndicator(series, p), 2.0, p)); // 2.0 es un multiplicador estándar

        INDICATOR_MAP.put("keltner: lower",(series, p) -> new KeltnerChannelLowerIndicator(
                        new KeltnerChannelMiddleIndicator(series, p), 2.0, p)); // 2.0 es un multiplicador estándar

        INDICATOR_MAP.put("keltner: facade",(series, p) -> {
                    KeltnerChannelFacade facade = new KeltnerChannelFacade(series, p, p, 2.0); // 2.0 como multiplicador
                    return new CompositeIndicator<>(Map.of(
                            "middle", facade.middle(),
                            "upper", facade.upper(),
                            "lower", facade.lower()
                    ));
                });

        // Pivot Points
        INDICATOR_MAP.put("pivot: standard",(series, p) -> new PivotPointIndicator(series, TimeLevel.DAY)); // Cambia TimeLevel según sea necesario

        INDICATOR_MAP.put("standard_reversal: standard_reversal",(series, p) -> new StandardReversalIndicator(
                        new PivotPointIndicator(series, TimeLevel.DAY), PivotLevel.RESISTANCE_1)); // Cambia PivotLevel según sea necesario

        INDICATOR_MAP.put("demark: demark",(series, p) -> new DeMarkPivotPointIndicator(series, TimeLevel.DAY)); // Cambia TimeLevel según sea necesario

        INDICATOR_MAP.put("demark_reversal: demark_reversal",(series, p) -> new DeMarkReversalIndicator(
                        new DeMarkPivotPointIndicator(series, TimeLevel.DAY), DeMarkReversalIndicator.DeMarkPivotLevel.RESISTANCE));

        INDICATOR_MAP.put("fibonacci: fibonacci",(series, p) -> new FibonacciReversalIndicator(
                        new PivotPointIndicator(series, TimeLevel.DAY), FibonacciReversalIndicator.FibonacciFactor.FACTOR_1,
                        FibonacciReversalIndicator.FibReversalTyp.RESISTANCE));

        // Statistical Indicators
        INDICATOR_MAP.put("variance: variance",(series, p) -> new VarianceIndicator(new ClosePriceIndicator(series), p));

        INDICATOR_MAP.put("covariance: covariance",(series, p) -> new CovarianceIndicator(
                        new ClosePriceIndicator(series), new VolumeIndicator(series), p));

        INDICATOR_MAP.put("mean_deviation: mean_deviation",(series, p) -> new MeanDeviationIndicator(new ClosePriceIndicator(series), p));

        INDICATOR_MAP.put("standard_deviation: standard_deviation",(series, p) -> new StandardDeviationIndicator(new ClosePriceIndicator(series), p));

        INDICATOR_MAP.put("standard_error: standard_error",(series, p) -> new StandardErrorIndicator(new ClosePriceIndicator(series), p));

        INDICATOR_MAP.put("sigma: sigma",(series, p) -> new SigmaIndicator(new ClosePriceIndicator(series), p));

        INDICATOR_MAP.put("correlation_coefficient: correlation_coefficient",(series, p) -> new CorrelationCoefficientIndicator(
                        new ClosePriceIndicator(series), new VolumeIndicator(series), p));

        INDICATOR_MAP.put("pearson_correlation: pearson_correlation",(series, p) -> new PearsonCorrelationIndicator(
                        new ClosePriceIndicator(series), new VolumeIndicator(series), p));

        INDICATOR_MAP.put("periodical_growth_rate: periodical_growth_rate",(series, p) -> new PeriodicalGrowthRateIndicator(new ClosePriceIndicator(series), p));

        INDICATOR_MAP.put("simple_linear_regression: simple_linear_regression",(series, p) -> new SimpleLinearRegressionIndicator(
                        new ClosePriceIndicator(series), p, SimpleLinearRegressionIndicator.SimpleLinearRegressionType.Y));

        // SuperTrend Indicators
        INDICATOR_MAP.put("supertrend: upper_band",(series, p)
                -> new SuperTrendUpperBandIndicator(series, new ATRIndicator(series, p), 3.0)); // Multiplicador 3.0 por defecto

        INDICATOR_MAP.put("supertrend: lower_band",(series, p)
                -> new SuperTrendLowerBandIndicator(series, new ATRIndicator(series, p), 3.0)); // Multiplicador 3.0 por defecto

        INDICATOR_MAP.put("supertrend: indicator",(series, p)
                -> new SuperTrendIndicator(series, p, 3.0)); // Multiplicador 3.0 por defecto

        // Trend Indicators
        INDICATOR_MAP.put("up_trend: up_trend",(series, p)
                -> new WrapperIndicator<>(new UpTrendIndicator(series, p))); // Convierte el valor booleano a Num

        INDICATOR_MAP.put("down_trend: down_trend",(series, p)
                -> new WrapperIndicator<>(new DownTrendIndicator(series, p))); // Convierte el valor booleano a Num

        // Volume-Based Indicators
        INDICATOR_MAP.put("accumulation_distribution: accumulation_distribution",(series, p)
                -> new TimeSegmentedVolumeIndicator(series, p));

        INDICATOR_MAP.put("accumulation_distribution: accumulation_distribution",(series, p)
                -> new AccumulationDistributionIndicator(series));

        INDICATOR_MAP.put("chaikin_money_flow: chaikin_money_flow",(series, p)
                -> new ChaikinMoneyFlowIndicator(series, p));

        INDICATOR_MAP.put("chaikin_oscillator: chaikin_oscillator",
                (series, p) -> new ChaikinOscillatorIndicator(series, 3, 10)); // Parámetros estándar

        INDICATOR_MAP.put("iii: iii",
                (series, p) -> new IIIIndicator(series));

        INDICATOR_MAP.put("mvwap: mvwap",
                (series, p) -> new MVWAPIndicator(new VWAPIndicator(series, p), p));

        INDICATOR_MAP.put("nvi: nvi",
                (series, p) -> new NVIIndicator(series));

        INDICATOR_MAP.put("pvi: pvi",
                (series, p) -> new PVIIndicator(series));

        INDICATOR_MAP.put("relative_volume_std_dev: relative_volume_std_dev",
                (series, p) -> new RelativeVolumeStandardDeviationIndicator(series, p));

        INDICATOR_MAP.put("rocv: rocv",
                (series, p) -> new ROCVIndicator(series, p));

        INDICATOR_MAP.put("obv: obv", (series, p) -> new OnBalanceVolumeIndicator(series));
        INDICATOR_MAP.put("vwap: vwap", (series, p) -> new VWAPIndicator(series, p));
        INDICATOR_MAP.put("mfi: mfi", (series, p) -> new MoneyFlowIndexIndicator(series, p));



        //REsto de indicadores base

        INDICATOR_MAP.put("coppock: coppock",
                (series, p) -> new CoppockCurveIndicator(new ClosePriceIndicator(series), 14, 11, 10)); // Parámetros estándar

        INDICATOR_MAP.put("intraday: intraday",
                (series, p) -> new IntraDayMomentumIndexIndicator(series, p));

        INDICATOR_MAP.put("kst: kst",
                (series, p) -> new KSTIndicator(new ClosePriceIndicator(series), 10, 10, 15, 10, 20, 10, 30, 15));

        INDICATOR_MAP.put("chop: chop",
                (series, p) -> new ChopIndicator(series, p, 100)); // Escala estándar a 100

        // Statistical and Trend Indicators
        INDICATOR_MAP.put("lwma: lwma",
                (series, p) -> new LWMAIndicator(new ClosePriceIndicator(series), p)); // Línea ponderada

        INDICATOR_MAP.put("mass: mass",
                (series, p) -> new MassIndexIndicator(series, 9, p)); // Uso común con 9 EMA

        INDICATOR_MAP.put("mma: mma",
                (series, p) -> new MMAIndicator(new ClosePriceIndicator(series), p)); // MMA

        INDICATOR_MAP.put("ppo: ppo",
                (series, p) -> new PPOIndicator(new ClosePriceIndicator(series), 12, 26)); // PPO estándar

        INDICATOR_MAP.put("pvo: pvo",
                (series, p) -> new PVOIndicator(series, 12, 26)); // PVO basado en volumen

        INDICATOR_MAP.put("ravi: ravi", (series, p) -> {
            int fastSmaPeriod = p > 0 ? p : 7; // Valor predeterminado
            int slowSmaPeriod = p > 0 ? p * 2 : 14; // Valor predeterminado
            return new RAVIIndicator(
                    new ClosePriceIndicator(series), // Indicador base
                    fastSmaPeriod,                   // Período corto
                    slowSmaPeriod                    // Período largo
            );
        });


        INDICATOR_MAP.put("rswing: high",
                (series, p) -> new RecentSwingHighIndicator(series, p)); // Reciente máximo

        INDICATOR_MAP.put("rswing: low",
                (series, p) -> new RecentSwingLowIndicator(series, p)); // Reciente mínimo

        INDICATOR_MAP.put("rwi: high",
                (series, p) -> new RWIHighIndicator(series, p)); // Random Walk Index (alto)

        INDICATOR_MAP.put("fisher: fisher",
                (series, p) -> new FisherIndicator(new MedianPriceIndicator(series), p));

        INDICATOR_MAP.put("hma: hma",
                (series, p) -> new HMAIndicator(new ClosePriceIndicator(series), p));


        // Indicator
        INDICATOR_MAP.put("dpo: dpo", (series, p) -> new DPOIndicator(new ClosePriceIndicator(series), p));
        INDICATOR_MAP.put("chandelier: exit_long", (series, p) -> new ChandelierExitLongIndicator(series, p, p * 2));
        INDICATOR_MAP.put("chandelier: exit_short", (series, p) -> new ChandelierExitShortIndicator(series, p, p * 2));

        INDICATOR_MAP.put("rwilow: rwi_low", (series, p) -> new RWILowIndicator(series, p));
        INDICATOR_MAP.put("distancefromma: dfma", (series, p) -> new DistanceFromMAIndicator(series, new SMAIndicator(new ClosePriceIndicator(series), p)));

        INDICATOR_MAP.put("kalman: filter", (series, p) -> new KalmanFilterIndicator(new ClosePriceIndicator(series)));

        // Williams %R Indicator
        INDICATOR_MAP.put("williams_r: williams_r",
                (series, p) -> new WilliamsRIndicator(series, p)); // Indicador de Momento de Williams %R

    }

    public static Indicator<Num> createIndicator(BarSeries series, String rawName, int period) {
        String key = rawName.trim().toLowerCase();
        IndicatorCreator creator = INDICATOR_MAP.get(key);
        if (creator != null) {
            return creator.apply(series, period);
        }
        // Fallback si no lo encuentras
        return new ClosePriceIndicator(series);
    }
}
