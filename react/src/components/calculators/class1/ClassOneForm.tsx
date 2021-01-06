import React, {useContext} from "react"
import Details from "../../Details";
import {ClassOneContext} from "./ClassOneContext";
import Class1Table from "./Class1Table";
import CurrencyInput from "../../helpers/gov-design-system/CurrencyInput";
import TextInput from "../../helpers/formhelpers/TextInput";

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

      <div className="container">
        <div className="container half">
          <div className="form-group item">
            <TextInput
              labelText="NI paid net contributions"
              name="niPaidNet"
              inputClassName="form-control full"
              inputValue={niPaidNet}
              error={errors?.niPaidNet}
              onChangeCallback={(e: React.ChangeEvent<HTMLInputElement>) => setNiPaidNet(e.target.value)}
            />
          </div>
        </div>

        <div className="container half">
          <div className="form-group item">
            <TextInput
              labelText="NI paid employee contributions"
              name="niPaidEmployee"
              inputClassName="form-control full"
              inputValue={niPaidEmployee}
              error={errors?.niPaidEmployee}
              onChangeCallback={(e: React.ChangeEvent<HTMLInputElement>) => setNiPaidEmployee(e.target.value)}
            />
          </div>
        </div>
      </div>

      <button className="govuk-button govuk-!-width-one-quarter" type="submit">
        Calculate
      </button>
    </form>
  )
}