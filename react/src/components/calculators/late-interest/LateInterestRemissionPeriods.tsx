import React, {useContext} from 'react'

// components
import {DateRange} from '../shared/DateRange'
import {LateInterestContext} from './LateInterestContext'

function LateInterestRemissionPeridos() {
  const {
    setDateRange,
    errors
  } = useContext(LateInterestContext)
  return (
    <div className="section--top section-outer--top">
      <h2 className="section-heading">Remission period</h2>

      <div className="section--top">
        <DateRange
          id="remission-period"
          legends={{
            from: "Start (optional)",
            to: "End (optional)"
          }}
          setDateRange={setDateRange}
          errors={errors}
        />
      </div>

    </div>
  )
}

export default LateInterestRemissionPeridos