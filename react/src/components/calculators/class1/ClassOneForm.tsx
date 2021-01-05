import React, {useContext} from "react"
import Details from "../../Details";
import {ClassOneContext} from "../../../services/ClassOneContext";
import Class1Table from "./Class1Table";
import CurrencyInput from "../../helpers/gov-design-system/CurrencyInput";

export default function ClassOneForm(props: any) {
  const { handleSubmit, handleDetailsChange, resetTotals } = props
  const {
    details,
    niPaidNet,
    setNiPaidNet,
    niPaidEmployee,
    setNiPaidEmployee,
    errors
  } = useContext(ClassOneContext)
  return (
    <form onSubmit={handleSubmit} noValidate>
      <Details
        details={details}
        handleChange={handleDetailsChange}
      />
      <Class1Table
        resetTotals={resetTotals}
      />
      <CurrencyInput
        label="NI paid net contributions"
        id="niPaidNet"
        value={niPaidNet}
        error={errors?.niPaidNet}
        onChange={(e: React.ChangeEvent<HTMLInputElement>) => setNiPaidNet(e.target.value)}
      />
      <CurrencyInput
        label="NI paid employee contributions"
        id="niPaidEmployee"
        value={niPaidEmployee}
        error={errors?.niPaidEmployee}
        onChange={(e: React.ChangeEvent<HTMLInputElement>) => setNiPaidEmployee(e.target.value)}
      />
      <button className="govuk-button govuk-!-width-one-quarter" type="submit">
        Calculate
      </button>
    </form>
  )
}