import React from "react";
import {BootstrapTable, TableHeaderColumn} from "react-bootstrap-table";
import axios from "axios";
import update from "react-addons-update";
import Jumbotron from "react-bootstrap/lib/Jumbotron";

class LocationSettings extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            locations: [],
            admin: (document.getElementById('admin').value == 'true')
        };
    };

    componentDidMount = () => {
        this.loadData();
    };

    loadData = () => {
        let t = this;
        axios.get(document.getElementById('settingsBackendUrl').value)
            .then(function (sensorLocation) {
                    const newState = update(t.state, {
                        locations: {$set: sensorLocation.data},
                    });

                    t.setState(newState);
                    console.log(JSON.stringify(newState));
                }
            )
            .catch(function (error) {
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
        let deleteUrl = document.getElementById('settingsDeleteLocation').value + row;

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
    render = () => {
        return <Jumbotron bsClass="bc-measurement-box">
            <h3>Locations</h3>
            <BootstrapTable data={this.state.locations}
                            cellEdit={{
                                mode: 'click',
                                blurToSave: true,
                                afterSaveCell: this.onAfterSaveLocationCell
                            }}
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
        </Jumbotron>
    }
}

export default LocationSettings