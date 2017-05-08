import React from "react";
import Col from "react-bootstrap/lib/Col";
import axios from "axios";
import {BootstrapTable, TableHeaderColumn} from "react-bootstrap-table";
import update from "react-addons-update";

class HomeCenterSettings extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            bcSensorLocations: [],
            bcActiveSensors: [],
            admin: (document.getElementById('admin').value == 'true')
        };
    };

    componentDidMount = () => {
        this.loadData();
    };

    loadData = () => {
        let t = this;
        axios.all([
            axios.get(document.getElementById('settingsBackendUrl').value),
            axios.get(document.getElementById('bcSensorReading').value)
        ]).then(axios.spread(function (sensorLocation, activeSensors) {
            const newState = update(t.state, {
                bcSensorLocations: {$set: sensorLocation.data},
                activeSensors: {
                    $set: activeSensors.data.map(d => {
                        let nd = {
                            measuredPhenomenon: d.measuredPhenomenon,
                            address: d.location.address,
                            key: d.location.address + ":" + d.measuredPhenomenon
                        };
                        return nd;
                    })
                },
            });

            t.setState(newState);
            console.log(JSON.stringify(newState));
        })).catch(function (error) {
            console.log(error);
        });
    };

    onAfterSaveLocationCell = (row) => {
        this.postLocationSettings(row.address, row.label);
    };

    onAddLocationRow = (row) => {
        if (row.address.length > 0 && row.label.length > 0) {
            this.postLocationSettings(row.address, row.label);
        }
    };

    onDeleteLocationRow = (row) => {
        let t = this;
        let deleteUrl = document.getElementById('settingsDeleteLocation').value + "/" + row;

        axios.delete(deleteUrl).then(function () {
            t.loadData();
        });
    };

    postLocationSettings = (address, label) => {
        let t = this;
        let postUrl = document.getElementById('settingsBackendUrl').value;
        axios.post(postUrl, {address: address, label: label})
            .then(function () {
                t.loadData();
            });
    };

    onCleanData = (address, measuredPhenomenon) => {
        let t = this;
        let deleteUrl = document.getElementById('bcSensorReading').value +
            address + "/" + measuredPhenomenon;

        axios.delete(deleteUrl).then(function () {
            t.loadData();
        });
    };

    cleanDataButtonFormatter = (cell, data, rowIndex) => {
        return <button
            type="button"
            onClick={this.onCleanData.bind(
                this,
                data.address,
                data.measuredPhenomenon
            )}
        >
            Clean data
        </button>
    };

    render = () => {
        const cellEditProp = {
            mode: 'click',
            blurToSave: true,
            afterSaveCell: this.onAfterSaveLocationCell
        };

        return <div>
            <Col xs={6} md={5}>
                <BootstrapTable data={this.state.bcSensorLocations}
                                cellEdit={cellEditProp}
                                striped
                                hover
                                deleteRow={this.state.admin}
                                insertRow={this.state.admin}
                                selectRow={{mode: 'radio'}}
                                options={{
                                    onDeleteRow: this.onDeleteLocationRow,
                                    onAddRow: this.onAddLocationRow
                                }}
                >
                    <TableHeaderColumn isKey dataField='address'>Address</TableHeaderColumn>
                    <TableHeaderColumn
                        dataField='label'
                        editable={this.state.admin}>Label</TableHeaderColumn>
                </BootstrapTable>
            </Col>
            <Col xs={6} md={5}>
                <BootstrapTable data={this.state.activeSensors}
                                striped
                                hover
                                selectRow={{mode: 'none'}}
                >
                    <TableHeaderColumn isKey dataField='key'
                                       dataFormat={(cell, data, rowIndex) => {
                                           return cell.split(":")[0]
                                       }}>
                        Location</TableHeaderColumn>
                    <TableHeaderColumn dataField='measuredPhenomenon'>Phenomenon</TableHeaderColumn>
                    <TableHeaderColumn
                        dataField='address'
                        dataFormat={this.cleanDataButtonFormatter.bind(this)}
                        hiddenHeader={true}
                    />
                </BootstrapTable>
            </Col>
        </div>
    }
}

export default HomeCenterSettings