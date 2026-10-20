import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import ChartTooltip from './ChartTooltip'
import { axisProps, GRID, SERIES } from './chartTheme'

/** Single-measure line over time: 2px line, 8px markers, crosshair tooltip. */
export default function LineChartView({ data, xKey, series, valueFormat = (v) => v, labelFormat, height = 240 }) {
  return (
    <div style={{ height }} role="img" aria-label={`${series.map((s) => s.label).join(', ')} over time`}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 8, right: 16, bottom: 4, left: 0 }}>
          <CartesianGrid stroke={GRID} vertical={false} />
          <XAxis dataKey={xKey} {...axisProps} tickFormatter={labelFormat} interval="preserveStartEnd" />
          <YAxis {...axisProps} tickFormatter={valueFormat} allowDecimals={false} width={48} />
          <Tooltip cursor={{ stroke: '#a3aab5', strokeWidth: 1 }} content={<ChartTooltip labelFormatter={labelFormat} valueFormatter={valueFormat} />} />
          {series.map((s, i) => (
            <Line key={s.key} type="linear" dataKey={s.key} name={s.label} stroke={s.color ?? SERIES[i]} strokeWidth={2}
              dot={{ r: 4, strokeWidth: 2, fill: '#fff' }} activeDot={{ r: 5 }} isAnimationActive={false} />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
