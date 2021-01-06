import React, {useContext} from 'react'
import {periodValueToLabel, fcn, PeriodValue, periods} from '../../../config';

// types
import { ClassOneEarningsProps } from '../../../interfaces'

// components
import TextInput from '../../helpers/formhelpers/TextInput'

import numeral from 'numeral'
import 'numeral/locales/en-gb';
import {ClassOneContext} from "./ClassOneContext";

numeral.locale('en-gb');

function ClassOneEarningsTable(props: ClassOneEarningsProps) {
  const { rows } = useContext(ClassOneContext)
  return (
    <table className="contribution-details">
      <thead>
        <tr className="clear">
          <th className="lg" colSpan={3}><span>Contribution payment details</span></th>
        </tr>
        <tr>
          <th><strong>Select period</strong></th>
          <th><strong>Row number</strong></th>
          <th><strong>Select NI category letter</strong></th>
          <th><strong>Enter gross pay</strong></th>
        </tr>
      </thead>
      
      <tbody>
        {rows.map((r, i) => (
          <tr className={props.activeRowID === r.id ? "active" : ""} key={r.id} id={r.id}>
            <td className="input">
              {props.handleSelectChange ?
                <>
                  <label className="govuk-visually-hidden" htmlFor={`row${i}-period`}>Period</label>
                  <select name="period" value={r.period} onChange={(e) => props.handleSelectChange?.(r, e)} className="borderless" id={`row${i}-period`}>
                    {periods.map((p: PeriodValue, i) => (
                      <option key={i} value={p}>{periodValueToLabel(p)}</option>
                    ))}
                  </select>
                </>
              :
              <div>{periodValueToLabel(r.period)}</div>
              }
            </td>

            {/* Row number */}
            <td className="input">
              <TextInput
                hiddenLabel={true}
                name={`${r.id}-number`}
                labelText="Row number (optional)"
                inputClassName="number"
                inputValue={r.number}
                placeholderText="Enter the row number (optional)"
                onChangeCallback={(e) => props.handleChange?.(r, e)}
              />
            </td>

            {/* Category */}
            <td className="input">
              {props.handleSelectChange ?
                <>
                  <label className="govuk-visually-hidden" htmlFor={`row${i}-category`}>Category</label>
                  <select name="category" value={r.category} onChange={(e) => props.handleSelectChange?.(r, e)} className="borderless" id={`row${i}-category`}>
                    {props.taxYear.categories.map((c, i) => (
                      <option key={i} value={c}>{fcn(c)}</option>
                    ))}
                  </select>
                </>
              : 
              <div>{r.category}</div>
              }
            </td>

            {/* Gross Pay */}
            <td className={
              `input ${props.rowsErrors?.[`${r.id}`]?.['gross'] ? "error-cell" : ""}`}>
              {props.handleChange ?
                <>
                  <TextInput
                    hiddenLabel={true}
                    name={`${r.id}-gross`}
                    labelText="Gross pay"
                    inputClassName="gross-pay"
                    inputValue={r.gross}
                    placeholderText="Enter the gross pay amount"
                    onChangeCallback={(e) => props.handleChange?.(r, e)}
                  />
                </>
              :
              <div>{r.gross}</div>
              }
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export default ClassOneEarningsTable