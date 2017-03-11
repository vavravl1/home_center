import React from "react";
import Col from "react-bootstrap/lib/Col";
import FormControl from "react-bootstrap/lib/FormControl";
import Button from "react-bootstrap/lib/Button";
import axios from "axios";
import {BootstrapTable, TableHeaderColumn} from "react-bootstrap-table";
import update from "react-addons-update";

class HomeCenterSettings extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            bcSensorLocations: []
        };
    };

    componentDidMount = () => {
        let t = this;
        axios.all([
            axios.get('/settings/bc/sensorLocation'),
        ]).then(axios.spread(function (sensorLocation) {
            const newState = update(t.state, {
                bcSensorLocations: {$set: sensorLocation.data},
            });

            t.setState(newState);
            console.log(JSON.stringify(newState));
        })).catch(function (error) {
            console.log(error);
        });
    };

    onAfterSaveCell = (row, cellName, cellValue) => {
        console.log('Storing change: ' + JSON.stringify(row) + ' ' + ' cellName: ' +  cellName + '; value=' + cellValue);

        let csrfTokenName = document.getElementById('csrf_token_name').value;
        let csrfTokenValue = document.getElementById('csrf_token_value').value;

        let postUrl = '/settings/bc/sensorLocation' + '?' + csrfTokenName + '=' + csrfTokenValue;
        axios({
            method: 'post',
            url: postUrl,
            data: {
                location: row.location,
                label: row.label
            }
        });
    };

    render = () => {
        let csrfTokenName = document.getElementById('csrf_token_name').value;
        let csrfTokenValue = document.getElementById('csrf_token_value').value;

        const cellEditProp = {
            mode: 'click',
            blurToSave: true,
            afterSaveCell: this.onAfterSaveCell
        };

        return <Col xs={6} md={5}>
            <BootstrapTable data={this.state.bcSensorLocations} cellEdit={cellEditProp} striped hover>
                <TableHeaderColumn isKey dataField='location'>Location</TableHeaderColumn>
                <TableHeaderColumn dataField='label'>Label</TableHeaderColumn>
            </BootstrapTable>


                <div style={{float: 'left', 'marginRight': '1em', 'marginLeft': '1em'}}>
                    <FormControl
                        id="newLocation"
                        type="text"
                        label="Location"
                        placeholder="Enter location"
                    />
                </div>
                <div style={{float: 'left', 'marginRight': '1em'}}>
                    <FormControl
                        id="newLabel"
                        type="text"
                        label="Label"
                        placeholder="Enter label"
                    />
                </div>
            <Button type="submit">
                Add new
            </Button>
        </Col>
    }
}

export default HomeCenterSettings