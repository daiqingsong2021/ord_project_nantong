/*
 * Activiti Modeler component part of the Activiti project
 * Copyright 2005-2014 Alfresco Software, Ltd. All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

/*
 * Condition expression
 */

var KisBpmConditionExpressionCtrl = [ '$scope', '$modal', function($scope, $modal) {

    // Config for the modal window
    var opts = {
        template:  'editor-app/configuration/properties/condition-expression-popup.html?version=' + Date.now(),
        backdrop : false,
        keyboard : false,
        scope: $scope
    };

    // Open the dialog
    $modal(opts);
}];

var KisBpmConditionExpressionPopupCtrl = [ '$scope', '$translate', '$http', function($scope, $translate, $http) {

	// Put json representing condition on scope
    if ($scope.property.value !== undefined && $scope.property.value !== null) {
        $scope.conditionExpression = {value: $scope.property.value};
    } else {
        $scope.conditionExpression = {value: ''};
    }

    var shapes = $scope.modelData.model.childShapes;
    if(shapes){
        for(var i = 0; i < shapes.length; i++){
            if(shapes[i].resourceId == $scope.selectedShape.resourceId){
                $scope.targetResourceId = shapes[i].target.resourceId;//.replace(/-/g,"_");
            }
        }
    }

    $scope.save = function() {
        $scope.property.value = $scope.conditionExpression.value;
        $scope.updatePropertyInModel($scope.property);
        $scope.close();
    };

    // Close button handler
    $scope.close = function() {
        $scope.property.mode = 'read';
        $scope.$hide();
    };

    // Close button handler
    $scope.getNextActivityIdExpression  = function() {debugger
        if($scope.targetResourceId){
            $scope.conditionExpression.value = "${expr.existActivity(nextActPart, '" + $scope.targetResourceId + "')}";
        }else{
            alert("请先保存并关闭流程设计页面，重新打开流程设计！")
        }
    }
}];