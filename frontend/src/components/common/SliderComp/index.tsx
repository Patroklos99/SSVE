import React, { useState } from 'react';
import Box from '@mui/material/Box';
import Slider from '@mui/material/Slider';
import CarFilterUtil from '../../../util/CarFilterUtil';
import ICar from '../../../types/Car';
import { green } from '@mui/material/colors';
import { createTheme, ThemeProvider } from '@mui/material/styles'


const theme = createTheme({
    palette : {
        primary: {
            main: green[700],

        },
    },
});


const marks = [

    {
        value:0,
        label:'0$',
    },

    {
        value:100000,
        
    },

    {
        value:200000,
        
    },

    {
        value:300000,
        label:'300 000$',
    },

];

function valueText(value: number) {
    return `{value}`;
}


export default function SliderComp(props: { minValue: number | undefined; maxValue: number | undefined; }) : any{

    const [price, setPrice] = useState()

    const filterSlider = (event: any, value: any) => {

        let cars: ICar[] = CarFilterUtil.getAll();
        for ( let i = 0; i < cars.length; i++) {
            if ( cars[i].price <= value) {
                CarFilterUtil.addValueToInclude(i,"prix");
            } else {
                CarFilterUtil.removeValueFromInclude(i,"prix");
            }
        }
        setPrice(value.target.valueAsNumber);
    };




    return (
        <div>
            <Box sx={{ width: 4/5 }}>
                <ThemeProvider theme={theme}>
                <Slider
                    className='priceSlider'
                    min={0}
                    max={300000}
                    value={price}
                    aria-label="price slider"
                    onChange={filterSlider}
                    getAriaValueText={valueText}
                    valueLabelDisplay="on"
                    aria-labelledby="non-linear-slider"
                    step={10000}
                    marks={marks}
                    color='primary'
                />
                </ThemeProvider>
            </Box>
        </div>
    )
}

