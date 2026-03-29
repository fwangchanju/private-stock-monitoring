#!/bin/bash
chmod +x gradlew
./gradlew test \
  --tests 'dev.eolmae.psms.collector.Ka20001MarketOverviewTest' \
  --tests 'dev.eolmae.psms.collector.Ka10051InvestorTradingSummaryTest' \
  --tests 'dev.eolmae.psms.collector.Ka10014ShortSellingTest' \
  --tests 'dev.eolmae.psms.collector.Ka10099StockMasterTest' \
  --tests 'dev.eolmae.psms.collector.Ka90003ProgramTradingRankingTest' \
  --tests 'dev.eolmae.psms.collector.Ka90008ProgramTradingHistoryTest'
