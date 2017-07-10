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

    execute = (actuator, command) => {
        const commandParameterField = document.getElementById(
            'input_value_' +
            command.name + '_' +
            actuator.location.address + '_' +
            actuator.name
        );

        let t = this;
        let postUrl = document.getElementById('actuatorsUrl').value + actuator.location.address + '/' + actuator.name;
        axios.post(postUrl, {
            name: command.name,
            requiredArguments: !!commandParameterField ?
                [{
                    name: command.requiredArguments[0].name,
                    unit: command.requiredArguments[0].unit,
                    value: commandParameterField.value
                }] : []
        })
            .then(function () {
                t.loadData();
            });

        return false;
    };

    handlePressedEnterOnText = (actuator, command, target) => {
        if (target.charCode == 13) {
            this.execute(actuator, command);
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
                        actuator,
                        command
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
                        actuator,
                        command
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