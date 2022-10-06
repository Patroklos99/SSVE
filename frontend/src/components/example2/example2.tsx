import React, { useState, ChangeEvent } from "react";
import { Link } from 'react-router-dom';
import ExampleService from "../../services/ExampleServices";
import CarData from "../../types/Car";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";

const AddCar: React.FC = () => {
    
  const initialCarState = {
    id: null,
    modelName: "", 
  };

  const [Car, setCar] = useState<CarData>(initialCarState);
  const [submitted, setSubmitted] = useState<boolean>(false);

  const handleInputChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setCar({ ...Car, [name]: value });
  };

  const saveTutorial = () => {
    var data = {
      modelName: Car.modelName
    };

    ExampleService.post(data)
      .then((response: any) => {
        setCar({
          id: response.data.id,
          modelName: response.data.modelName,
        });
        setSubmitted(true);
        console.log(response.data);
      })
      .catch((e: Error) => {
        console.log(e);
      });
  };

  const newCar = () => {
    setCar(initialCarState);
    setSubmitted(false);
  };

  return ( 
  <div className="submit-form">
  {submitted ? (
    <div>
      <h4>You submitted successfully!</h4>
      <button className="btn btn-success" onClick={newCar}>
        Add
      </button>
    </div>
  ) : (
    <div>
      <div className="form-group">
        <TextField
          type="text"
          className="form-control"
          id="modelName"
          required
          value={Car.modelName as string}
          onChange={handleInputChange}
          name="modelName"
          label="Model"
          variant="filled"
        />
      </div>

      <Button onClick={saveTutorial} variant="contained">
        Submit
      </Button>
      <div>
        <Link to="/">Back</Link>
      </div>
    </div>
  )}
</div>
);
};

export default AddCar;