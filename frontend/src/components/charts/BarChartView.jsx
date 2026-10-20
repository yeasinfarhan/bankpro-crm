import { Bar, BarChart, CartesianGrid, Cell, Legend, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import ChartTooltip from './ChartTooltip'
import { axisProps, barRadius, GRID, SERIES } from './chartTheme'

/**
 * Bar chart with one or more series (same unit, one axis — never dual-axis).
 * layout "horizontal" = vertical bars (time on x); "vertical" = horizontal bars (long category names).
 * colorFor(row) can override a bar's colour (e.g. neutral grey for "Lost").
 */
export default function BarChartView({ data, xKey, series, layout = 'horizontal', valueFormat = (v) => v, axisFormat, labelFormat, height = 260, reference, colorFor, categoryWidth = 130 }) {
  const horizontalBars = layout === 'vertical'
  const fmtAxis = axisFormat ?? valueFormat
  // Keep a reference line (e.g. 100% of target) inside the visible range.
  const domain = reference ? [0, (max) => Math.max(max, reference.value)] : undefined
  return (
    <div style={{ height }} role="img" aria-label={`${series.map((s) => s.label).join(', ')} by ${xKey}`}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} layout={layout} margin={{ top: reference ? 22 : 8, right: 24, bottom: 4, left: horizontalBars ? 8 : 0 }} barCategoryGap="28%" barGap={2}>
          <CartesianGrid stroke={GRID} horizontal={!horizontalBars} vertical={horizontalBars} />
          {horizontalBars ? (
            <>
              <XAxis type="number" {...axisProps} tickFormatter={fmtAxis} domain={domain} />
              <YAxis type="category" dataKey={xKey} {...axisProps} width={categoryWidth} tickFormatter={labelFormat} />
            </>
          ) : (
            <>
              <XAxis dataKey={xKey} {...axisProps} tickFormatter={labelFormat} interval="preserveStartEnd" />
              <YAxis {...axisProps} tickFormatter={fmtAxis} width={64} domain={domain} />
            </>
          )}
          <Tooltip cursor={{ fill: 'rgba(42,120,214,0.06)' }}
            content={<ChartTooltip labelFormatter={labelFormat} valueFormatter={(v, p) => (series.find((s) => s.key === p.dataKey)?.format ?? valueFormat)(v)} />} />
          {series.length > 1 && <Legend iconType="square" iconSize={10} wrapperStyle={{ fontSize: 12, color: '#5b6474' }} />}
          {reference && (
            <ReferenceLine {...(horizontalBars ? { x: reference.value } : { y: reference.value })} stroke="#5b6474" strokeWidth={1}
              label={{ value: reference.label, position: horizontalBars ? 'top' : 'right', fill: '#5b6474', fontSize: 11 }} />
          )}
          {series.map((s, i) => (
            <Bar key={s.key} dataKey={s.key} name={s.label} fill={s.color ?? SERIES[i]} maxBarSize={28}
              radius={horizontalBars ? barRadius.horizontal : barRadius.vertical} isAnimationActive={false}>
              {colorFor && data.map((row, j) => <Cell key={j} fill={colorFor(row) ?? s.color ?? SERIES[i]} />)}
            </Bar>
          ))}
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
