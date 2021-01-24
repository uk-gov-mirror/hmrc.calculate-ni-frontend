import React, {useContext} from 'react'

// components
import {DateRange} from '../shared/DateRange'
import {LateInterestContext} from './LateInterestContext'

function LateInterestRemissionPeridos() {
  const {
    setDateRange,
    errors,
    setValidateRemissionPeriods
  } = useContext(LateInterestContext)
  return (
    <div className="section--top section-outer--top">
      <h2 className="section-heading">Remission period</h2>

      <div className="section--top">
        <DateRange
          id="remissionPeriod"
          legends={{
            from: "Start",
            to: "End"
          }}
          setDateRange={setDateRange}
          errors={errors}
          isOptional={true}
          setValidateOptionalDateRange={setValidateRemissionPeriods}
        />
      </div>

    </div>
  )
}

export default LateInterestRemissionPeridos