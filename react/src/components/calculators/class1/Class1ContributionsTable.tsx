import React, {useContext} from 'react'
import {periodValueToLabel} from '../../../config';

// types
import {Row} from '../../../interfaces'

// components

import numeral from 'numeral'
import 'numeral/locales/en-gb';
import {ClassOneContext} from "./ClassOneContext";

numeral.locale('en-gb');

function ClassOneContributionsTable(props: {showBands: boolean}) {
  const { showBands } = props
  const { rows } = useContext(ClassOneContext)
  return (
    <table className="contribution-details">
      <thead>
        <tr className="clear">
          <th className="lg" colSpan={3}><span>Contribution payment details</span></th>
          {showBands && rows[0].bands &&
            <th className="border" colSpan={Object.keys(rows[0].bands).length}><span>Earnings</span></th>
          }
          <th className="border" colSpan={showBands && rows[0].bands ? 3 : 2}><span>Net contributions</span></th>
        </tr>
        <tr>
          <th><strong>Period</strong></th>
          <th><strong>Row number</strong></th>
          <th><strong>NI category letter</strong></th>
          <th><strong>Gross pay</strong></th>
          {/* Bands - by tax year, so we can just take the first band to map the rows */}
          {showBands && rows[0].bands && Object.keys(rows[0].bands).map(k =>
            <th key={k}>{k}</th>
          )}

          {showBands && rows[0].bands &&
            <th><strong>Total</strong></th>
          }
          <th><strong><abbr title="Employee">EE</abbr></strong></th>
          <th><strong><abbr title="Employer">ER</abbr></strong></th>
        </tr>
      </thead>
      
      <tbody>
        {rows.map((r: Row) => (
          <tr key={r.id}>
            <td>
              <div>{periodValueToLabel(r.period)}</div>
            </td>

            <td>
              {r.number}
            </td>

            <td>
              <div>{r.category}</div>
            </td>
            <td>
              <div>{r.gross}</div>
            </td>

            {showBands && r.bands && Object.keys(r.bands).map(k =>
              <td key={`${k}-val`}>{numeral(r.bands?.[k][0]).format('$0,0.00')}</td>
            )}

            {showBands && r.bands &&
              <td>
                {numeral(
                  (parseFloat(r.ee) + parseFloat(r.er)).toString()
                ).format('$0,0.00')}
              </td>
            }

            <td>{numeral(r.ee).format('$0,0.00')}</td>
            <td>{numeral(r.er).format('$0,0.00')}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export default ClassOneContributionsTable