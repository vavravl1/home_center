import React from "react";
import axios from "axios";
import update from "react-addons-update";
import {BootstrapTable, TableHeaderColumn} from "react-bootstrap-table";
import Jumbotron from "react-bootstrap/lib/Jumbotron";
import Col from "react-bootstrap/lib/Col";

class Actuator extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            actuators: []
        }
    };

    componentDidMount = () => {
        this.loadData(this);
    };

    loadData = () => {
        let t = this;
        axios.get(document.getElementById('actuatorsUrl').value)
            .then(function (actuators) {
                const newState = update(t.state, {
                    actuators: {
                        $set: actuators.data
                    },
                });
                t.setState(newState);
            }).catch(function (error) {
            console.log(error);
        });
    };

    execute = (locationAddress, actuatorName, commandName) => {
        const commandParameterField = document.getElementById(
            'input_value_' +
            commandName + '_' +
            locationAddress + '_' +
            actuatorName
        );

        let t = this;
        let postUrl = document.getElementById('actuatorsUrl').value + locationAddress + '/' + actuatorName;
        axios.post(postUrl, {
            name: commandName,
            requiredArguments: !!commandParameterField ?
                [{
                    name: 'param',
                    unit: 'x',
                    value: commandParameterField.value
                }] : []
        })
            .then(function () {
                t.loadData();
            });

        return false;
    };

    handlePressedEnterOnText = (locationAddress, actuatorName, commandName, target) => {
        if (target.charCode == 13) {
            this.execute(locationAddress, actuatorName, commandName);
        }
    };

    componentWillUnmount = () => {
    };

    requiredArgumentsRenderer = (actuator, cell, command, rowIndex) => {
        if (command.requiredArguments.length == 0) {
            return <button
                type="button"
                onClick={
                    this.execute.bind(
                        this,
                        actuator.location.address,
                        actuator.name,
                        command.name
                    )
                }>
                {command.name}
            </button>
        } else {
            return <input
                id={'input_value_' + command.name + '_' + actuator.location.address + '_' + actuator.name}
                type='text'
                placeholder={command.requiredArguments[0].value}
                onKeyPress={
                    this.handlePressedEnterOnText.bind(
                        this,
                        actuator.location.address,
                        actuator.name,
                        command.name
                    )
                }
            />
        }
    };

    actuatorView = (actuator) => {
        return <Jumbotron bsClass="bc-measurement-box">
            <h3>{actuator.name}</h3>
            <BootstrapTable data={actuator.supportedCommands}
                            striped
                            hover
                            selectRow={{mode: 'none'}}
            >
                <TableHeaderColumn isKey dataField='name'>Name</TableHeaderColumn>
                <TableHeaderColumn
                    dataField='requiredArguments'
                    dataFormat={this.requiredArgumentsRenderer.bind(this, actuator)}
                    hiddenHeader={true}
                />
            </BootstrapTable>
        </Jumbotron>

    };

    render = () => {
        return <div>
            <Col xs={6} md={5}>
                {this.state.actuators.map(actuator => this.actuatorView(actuator))}
            </Col>
        </div>
    };
}

Actuator.propTypes = {};


export default Actuator;